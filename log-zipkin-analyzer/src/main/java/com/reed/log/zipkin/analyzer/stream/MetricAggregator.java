package com.reed.log.zipkin.analyzer.stream;

import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.streams.kstream.Aggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reed.log.zipkin.analyzer.metric.MetricObj;
import com.reed.log.zipkin.analyzer.metric.MetricService;
import com.reed.log.zipkin.analyzer.pojo.ZipkinLog;
import com.reed.log.zipkin.analyzer.redis.MetricsCacheService;
import com.reed.log.zipkin.analyzer.tree.TreeObj;
import com.reed.log.zipkin.analyzer.tree.TreeParser;

/**
 * 聚合统计
 * @author reed
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class MetricAggregator implements Aggregator<String, TreeObj, MetricObj> {
	public static Logger logger = LoggerFactory.getLogger(MetricAggregator.class);
	@Autowired
	private MetricService metricService;

	@Autowired
	private MetricsCacheService cacheService;

	@Override
	public MetricObj apply(String key, TreeObj value, MetricObj aggregate) {
		try {
			metric(value);
			cache(value);
		} catch (Exception e) {
			logger.error("==========Kafka Stream Thread ERROR:{},{}============", e.getClass().getName(), e.getCause());
		}
		return null;
	}

	/**
	 * 记录统计
	 * @param value
	 */
	private void metric(TreeObj value) {
		if (value != null) {
			ObjectMapper mapper = new ObjectMapper();
			ZipkinLog m = mapper.convertValue(value.getBiz(), ZipkinLog.class);
			metricService.initMetricObj(m);
			if (value.getChildList() != null && !value.getChildList().isEmpty()) {
				(value.getChildList()).forEach((v) -> metric((TreeObj) v));
			}
		}
	}

	/**
	 * 缓存各应用统计项：appname-uri
	 * @param value
	 */
	private void cache(TreeObj value) {
		if (value != null) {
			List<TreeObj> list = new ArrayList<>();
			convert(value, list);
			List<TreeObj> r = TreeParser.getTreeList("-1", list);
			if (r != null && !r.isEmpty()) {
				r.forEach((t) -> {
					makeup(t);
					cacheService.cache(t.getApp(), t);
				});
			}
		}
	}

	/**
	 * 跟踪树转换为緩存统计项
	 * @param value 跟踪树
	 * @param list 缓存统计项集合
	 */
	private void convert(TreeObj value, List<TreeObj> list) {
		TreeObj t = new TreeObj<>();
		t.setId(value.getId());
		t.setApp(value.getApp());
		t.setName(value.getName());
		t.setParentId(value.getParentId());
		list.add(t);
		if (value.getChildList() != null && !value.getChildList().isEmpty()) {
			value.getChildList().forEach((v) -> {
				convert((TreeObj) v, list);
			});
		}
	}

	/**
	 * remove id and parentId
	 * @param t
	 */
	private void makeup(TreeObj t) {
		if (t != null) {
			t.setId(null);
			t.setParentId(null);
			if (t.getChildList() != null && !t.getChildList().isEmpty()) {
				for (TreeObj v : (List<TreeObj>) t.getChildList()) {
					makeup(v);
				}
			}
		}
	}
}
