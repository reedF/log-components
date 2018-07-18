package com.reed.log.zipkin.dependency.metric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;

@Configuration
public class MetricConfig {

	public static Logger logger = LoggerFactory.getLogger(MetricConfig.class);


	@Bean
	public MetricRegistry metrics(MetricSet sets) {
		MetricRegistry m = new MetricRegistry();
		m.registerAll(sets);
		return m;
	}


	@Bean
	public MetricSet metricSet() {
		Map<String, Metric> map = new ConcurrentHashMap<>();
		MetricSet set = () -> map;
		return set;
	}


}