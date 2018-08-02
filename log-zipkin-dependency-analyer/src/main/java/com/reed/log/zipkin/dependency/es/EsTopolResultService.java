package com.reed.log.zipkin.dependency.es;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.reed.log.zipkin.dependency.link.TopolLinker;
import com.reed.log.zipkin.dependency.metric.MetricService;

@Service
public class EsTopolResultService {

	@Autowired
	private EsTopolResultRepository esRepository;
	@Autowired
	private ElasticsearchTemplate esTemplate;

	@Autowired
	private MetricService metricService;

	public int saveAllCurrentResult() {
		List<EsTopolResult> r = findAllCurrentResult();
		if (r != null && !r.isEmpty()) {
			if (!esTemplate.indexExists(EsTopolResult.class)) {
				esTemplate.createIndex(EsTopolResult.class);
			}
			// create index mapping
			esTemplate.putMapping(EsTopolResult.class);
			esRepository.saveAll(r);
		}
		return r != null ? r.size() : 0;
	}

	public List<EsTopolResult> findAllCurrentResult() {
		List<EsTopolResult> r = null;
		Map<String, Set<Metric>> m = metricService.getAllMetrics();
		if (m != null && !m.isEmpty()) {
			r = new ArrayList<>();
			for (Map.Entry<String, Set<Metric>> entry : m.entrySet()) {
				if (entry != null && entry.getValue() != null) {
					EsTopolResult topol = new EsTopolResult();
					String key = entry.getKey();
					genTopol(topol, key);
					for (Metric v : entry.getValue()) {
						if (v instanceof Meter) {
							Meter qps = (Meter) v;
							topol.setCount(qps.getCount());
							topol.setQps(qps.getOneMinuteRate());
						}
						if (v instanceof Counter) {
							Counter c = (Counter) v;
							topol.setErrors(c.getCount());
						}
						if (v instanceof Histogram) {
							Histogram h = (Histogram) v;
							topol.setCost(h.getSnapshot().getMean());
						}
					}
					r.add(topol);
				}
			}
		}
		return r;
	}

	public void genTopol(EsTopolResult topol, String key) {
		if (key != null && key.contains(MetricService.M)) {
			String[] strs = key.split("\\|\\|");
			String[] parent = strs[0].split("\\|");
			String[] child = strs[1].split("\\|");
			String type = null;
			String p = parent[0];
			String pUrl = parent.length > 1 ? parent[1] : parent[0];
			if (pUrl != null && pUrl.contains(TopolLinker.TRACETYPETAG)) {
				type = pUrl.split(TopolLinker.TRACETYPETAG)[0];
				pUrl = pUrl.split(TopolLinker.TRACETYPETAG)[1];
			}
			String c = child[0];
			String childName = child.length > 1 ? child[1] : child[0];
			String cUrl = childName;
			if (childName != null) {
				if (childName.contains(TopolLinker.TRACETYPETAG)) {
					type = childName.split(TopolLinker.TRACETYPETAG)[0];
					cUrl = childName.split(TopolLinker.TRACETYPETAG)[1];
				} else {
					cUrl = pUrl;
				}
			}
			topol.setParentApp(p);
			topol.setParentUrl(pUrl);
			topol.setChildApp(c);
			topol.setChildUrl(cUrl);
			topol.setType(type);
		}
	}

}
