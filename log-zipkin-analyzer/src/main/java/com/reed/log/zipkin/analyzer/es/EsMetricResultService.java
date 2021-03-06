package com.reed.log.zipkin.analyzer.es;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.reed.log.zipkin.analyzer.metric.MetricObj;
import com.reed.log.zipkin.analyzer.metric.MetricService;
import com.reed.log.zipkin.analyzer.redis.MetricsCacheService;
import com.reed.log.zipkin.analyzer.tree.TreeObj;
import com.reed.log.zipkin.analyzer.tree.TreeParser;

@Service
public class EsMetricResultService {

	@Autowired
	private EsMetricResultRepository esRepository;
	@Autowired
	private MetricsCacheService cacheService;
	@Autowired
	private MetricService metricService;
	@Autowired
	private EsZipkinRepository esZipkinRepository;
	@Autowired
	private ElasticsearchTemplate esTemplate;

	public List<EsMetricResult> findAllCurrentResult() {
		List<EsMetricResult> list = new ArrayList<>();
		Set<String> apps = cacheService.getAppNames();
		if (apps != null) {
			for (String s : apps) {
				if (s != null) {
					Set<TreeObj> sets = cacheService.getAllMetricItem(s);
					getResult(sets, list);
				}
			}
		}
		return list;
	}

	public List<EsMetricResult> findCurrentResult(String app, String name) {
		List<EsMetricResult> list = new ArrayList<>();
		Set<TreeObj> sets = cacheService.getMetricItem(app, name);
		getResult(sets, list);
		return list;

	}

	public List<TreeObj> findTreeCurrentResult(String app, String name) {
		List<TreeObj> r = new ArrayList<>();
		List<EsMetricResult> list = null;
		if (StringUtils.isNotEmpty(app) && StringUtils.isNotEmpty(name)) {
			list = findCurrentResult(app, name);
		} else {
			list = findAllCurrentResult();
		}
		if (list != null && !list.isEmpty()) {
			for (EsMetricResult v : list) {
				if (v != null && v.getSpans() != null && !v.getSpans().isEmpty()) {
					for (MetricObj m : v.getSpans()) {
						TreeObj<MetricObj> t = new TreeObj<>();
						// root set app
						if (m.getPid() == -1) {
							t.setApp(m.getAppName());
						}
						t.setId(String.valueOf(m.getId()));
						t.setParentId(String.valueOf(m.getPid()));
						t.setBiz(m);
						t.setName(m.getName());
						r.add(t);
					}
				}
			}
		}
		return TreeParser.getTreeList("-1", r);

	}

	public int saveAllCurrentResult() {
		List<EsMetricResult> r = findAllCurrentResult();
		if (r != null && !r.isEmpty()) {
			if (!esTemplate.indexExists(EsMetricResult.class)) {
				esTemplate.createIndex(EsMetricResult.class);
			}
			// create index mapping
			esTemplate.putMapping(EsMetricResult.class);
			esRepository.saveAll(r);
		}
		// save children spans
		// saveChildrenSpans(r);
		return r.size();
	}

	/**
	 * 根据应用名、span名称等查询trace信息
	 * @param app
	 * @param type
	 * @param spanName
	 * @param start
	 * @param end
	 * @param pageable
	 * @return
	 */
	public Page<EsZipkin> findTrace(String app, String type, String spanName, String start, String end,
			Pageable pageable) {
		QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("app.keyword", app))
				.must(QueryBuilders.termQuery("type.keyword", type))
				.must(QueryBuilders.termQuery("children.name.keyword", spanName))
				.must(QueryBuilders.rangeQuery("createTime").gte(start).lte(end));
		Page<EsZipkin> page = esZipkinRepository.search(queryBuilder, pageable);
		Page<EsZipkin> pageResult = new PageImpl<>(page.getContent(), pageable, page.getTotalElements());
		return pageResult;
	}

	private void getResult(Set<TreeObj> sets, List<EsMetricResult> list) {
		if (sets != null) {
			for (TreeObj t : sets) {
				// 遍历所有跟踪树
				if (t != null) {
					EsMetricResult r = new EsMetricResult();
					r.setApp(t.getApp());
					r.setName(t.getName());
					r.setSpans(new ArrayList<>());
					getMetrics(t, null, r.getSpans());
					if (!r.getSpans().isEmpty()) {
						list.add(r);
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void getMetrics(TreeObj t, MetricObj parent, List<MetricObj> r) {
		if (t != null) {
			MetricObj o = new MetricObj();
			o.setId(System.nanoTime());
			o.setPid(parent == null ? -1 : parent.getId());
			o.setAppName(t.app);
			o.setName(t.name);
			setMetricValue(o);
			// 当前span存在统计值（表明有新请求）时才记录，并递归查询其子span，否则表明此span无新请求，不记录且也不可能存在对子span的请求
			if (o.getQps() != null) {
				r.add(o);
				// child node
				if (t.getChildList() != null && !t.getChildList().isEmpty()) {
					for (TreeObj v : (List<TreeObj>) t.getChildList()) {
						getMetrics(v, o, r);
					}
				}
			}
		}
	}

	private void setMetricValue(MetricObj o) {
		String qps = MetricService.genMetricName(o.getAppName(), o.getName(), null);
		String cost = MetricService.genMetricName(o.getAppName(), o.getName(), MetricService.HISTOGRAM_COST);
		String error = MetricService.genMetricName(o.getAppName(), o.getName(), MetricService.ERROR);
		Meter m = metricService.getMetric(qps, Meter.class);
		Histogram h = metricService.getMetric(cost, Histogram.class);
		Counter c = metricService.getMetric(error, Counter.class);
		if (m != null) {
			o.setTotal(m.getCount());
			o.setQps(m.getFifteenMinuteRate());
			o.setQpsMax(m.getFifteenMinuteRate());
		}
		if (h != null) {
			o.setCost(h.getSnapshot().getMean());
			o.setCostTp95(h.getSnapshot().get95thPercentile());
			o.setCostTp99(h.getSnapshot().get99thPercentile());
			o.setCostMax(h.getSnapshot().getMax());
		}
		if (c != null) {
			o.setErrors(c.getCount());
		}
		setMax(o, qps, cost);
	}

	private void setMax(MetricObj o, String qps, String cost) {
		if (o != null) {
			if (o.getQpsMax() != null) {
				o.setQpsMax(cacheService.addMaxValue(qps, o.getQpsMax()));
			}
			if (o.getCostMax() != null) {
				// Double d =
				// cacheService.addMaxValue(cost,o.getCostMax().doubleValue());
				// o.setCostMax(d == null ? null : d.longValue());
			}
		}
	}

	/**
	 * 保存子级span统计数据
	 * @param lists
	 */
	private void saveChildrenSpans(List<EsMetricResult> lists) {
		if (lists != null && lists.size() > 0) {
			List<EsMetricResult> children = new ArrayList<>();
			for (EsMetricResult r : lists) {
				List<MetricObj> spans = r.getSpans();
				if (spans != null && spans.size() > 0) {
					for (MetricObj m : spans) {
						if (m != null && m.getPid() > 0) {
							EsMetricResult child = new EsMetricResult();
							child.setId(System.nanoTime());
							List<MetricObj> ms = new ArrayList<>();
							ms.add(m);
							// String[] strs =
							// m.getAppName().split(MetricService.AT);
							child.setApp(m.getAppName());
							// if (m.getAppName().contains(MetricService.AT)) {
							// child.setServer(strs[1]);
							// }
							if (StringUtils.isNotBlank(m.getName()) && m.getName().contains(MetricService.M)) {
								String[] names = m.getName().split("\\|\\|");
								child.setName(names[1]);
							} else {
								child.setName(m.getName());
							}

							child.setSpans(ms);
							children.add(child);
						}
					}
				}
			}
			esRepository.saveAll(children);
		}
	}
}
