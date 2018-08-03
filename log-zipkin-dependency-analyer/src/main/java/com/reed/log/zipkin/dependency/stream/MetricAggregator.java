package com.reed.log.zipkin.dependency.stream;

import org.apache.kafka.streams.kstream.Aggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.reed.log.zipkin.dependency.link.TopolLink;
import com.reed.log.zipkin.dependency.metric.MetricService;

/**
 * 聚合统计
 * @author reed
 *
 */
public class MetricAggregator implements Aggregator<String, TopolLink, TopolLink> {
	public static Logger logger = LoggerFactory.getLogger(MetricAggregator.class);
	@Autowired
	private MetricService metricService;

	@Override
	public TopolLink apply(String key, TopolLink value, TopolLink aggregate) {
		try {
			metricService.metricsData(value);
		} catch (Exception e) {
			logger.error("==========Kafka Stream Thread ERROR:{},{}============", e.getClass().getName(), e.getCause());
			e.printStackTrace();
		}
		return null;
	}

}
