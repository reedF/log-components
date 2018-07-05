package com.reed.log.zipkin.analyzer.metric;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

/**
 * rest metrics
 * @author reed
 *
 */
public class MetricResetFilter implements MetricFilter {

	@Override
	public boolean matches(String name, Metric metric) {
		return checkMetric(metric);
	}

	public boolean checkMetric(Metric metric) {
		boolean r = false;
		if (metric instanceof Meter || metric instanceof Counter) {
			r = true;
		}
		return r;
	}
}
