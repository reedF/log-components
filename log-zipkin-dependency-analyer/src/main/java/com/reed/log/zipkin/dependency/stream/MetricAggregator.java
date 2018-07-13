package com.reed.log.zipkin.dependency.stream;

import org.apache.kafka.streams.kstream.Aggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reed.log.zipkin.dependency.link.TopolLink;

/**
 * 聚合统计
 * @author reed
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class MetricAggregator implements Aggregator<String, TopolLink, TopolLink> {
	public static Logger logger = LoggerFactory.getLogger(MetricAggregator.class);

	@Override
	public TopolLink apply(String key, TopolLink value, TopolLink aggregate) {

		logger.info("==========Kafka Stream Thread:{},{}============");

		return null;
	}

}
