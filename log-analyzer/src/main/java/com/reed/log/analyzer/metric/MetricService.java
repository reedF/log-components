package com.reed.log.analyzer.metric;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;

@Service
public class MetricService {

	private static final String meter = "meter.";

	@Autowired
	private MetricRegistry metrics;

	@Autowired
	private MetricSet metricSet;

	public void meter(String uri) {
		String k = meter + uri;
		if (metricSet != null && metricSet.getMetrics() != null) {
			Map<String, Metric> m = metricSet.getMetrics();
			if (m.containsKey(k)) {
				((Meter) m.get(k)).mark();
			} else {
				Meter item = metrics.meter(k);
				item.mark();
				m.put(k, item);
			}
		}
	}
}
