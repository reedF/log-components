package com.reed.log.test.metric;

import org.elasticsearch.metrics.JsonMetrics;
import org.elasticsearch.metrics.percolation.Notifier;

/**
 *
 */
public class SystemOutNotifier implements Notifier {

	@Override
	public void notify(JsonMetrics.JsonMetric jsonMetric, String id) {
		System.out.println(String.format("Metric %s of type %s at date %s matched with percolation id %s",
				jsonMetric.name(), jsonMetric.type(), jsonMetric.timestampAsDate(), id));
	}
}