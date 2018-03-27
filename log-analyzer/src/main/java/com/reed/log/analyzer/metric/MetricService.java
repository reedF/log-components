package com.reed.log.analyzer.metric;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;

@Service
public class MetricService {

	private static final String METER = "-";
	private static final String METER_HTTP = "HTTP-";
	private static final String METER_BIZ = "BIZ-CODE-";
	private static final String HISTOGRAM_COST = "COST-";
	private static final int SUCCESS = 200;

	@Autowired
	private MetricRegistry metrics;

	@Autowired
	private MetricSet metricSet;

	/**
	 * 根据名称创建或更新监控
	 * 
	 * @param name
	 */
	public void meter(String name) {
		String k = name;
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

	/**
	 * Http错误率
	 * 
	 * @param uri
	 * @param status
	 */
	public void meterError(String uri, int status) {
		if (status != SUCCESS) {
			meter(METER_HTTP + status + METER + uri);
		}
	}

	/**
	 * 业务状态码请求频率
	 * 
	 * @param uri
	 * @param code
	 */
	public void meterCode(String uri, Integer code) {
		if (code != null) {
			meter(METER_BIZ + code + METER + uri);
		}
	}

	/**
	 * 请求耗时,cost > 0 时才记录
	 * 
	 * @param uri
	 * @param cost
	 */
	public void histogramCost(String uri, int cost) {
		if (metricSet != null && metricSet.getMetrics() != null && cost > 0) {
			String k = HISTOGRAM_COST + uri;
			Map<String, Metric> m = metricSet.getMetrics();
			if (m.containsKey(k)) {
				((Histogram) m.get(k)).update(cost);
			} else {
				Histogram item = metrics.histogram(k);
				item.update(cost);
				m.put(k, item);
			}
		}
	}
}
