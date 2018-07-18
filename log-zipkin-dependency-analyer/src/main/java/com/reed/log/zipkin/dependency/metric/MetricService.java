package com.reed.log.zipkin.dependency.metric;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.reed.log.zipkin.dependency.link.TopolLink;

@Service
public class MetricService {

	public static final String M = "||";
	public static final String ERROR = "ERROR-";
	public static final String QPS = "QPS-";
	public static final String COST = "COST-";

	public static Logger logger = LoggerFactory.getLogger(MetricService.class);

	@Autowired
	private MetricRegistry metrics;

	@Autowired
	private MetricSet metricSet;

	public void metricsData(TopolLink topol) {
		if (topol != null) {
			meterQps(topol);
			meterCost(topol);
			meterError(topol);
		}
	}

	/**
	 * QPS
	 * 
	 */
	public Meter meterQps(TopolLink topol) {
		Meter meter = null;
		long count = topol.callCount();
		String k = genMetricName(topol, QPS);
		if (metricSet != null && metricSet.getMetrics() != null) {
			Map<String, Metric> m = metricSet.getMetrics();
			if (m.containsKey(k)) {
				meter = (Meter) m.get(k);
				meter.mark(count);
			} else {
				meter = metrics.meter(k);
				meter.mark(count);
				m.put(k, meter);
			}
		}
		return meter;
	}

	/**
	 * 请求耗时
	 * 
	 */
	public Histogram meterCost(TopolLink topol) {
		Histogram r = null;
		long cost = Double.valueOf(topol.cost()).longValue();
		if (metricSet != null && metricSet.getMetrics() != null) {
			String k = genMetricName(topol, COST);
			Map<String, Metric> m = metricSet.getMetrics();
			if (m.containsKey(k)) {
				r = ((Histogram) m.get(k));
				r.update(cost);
			} else {
				r = metrics.histogram(k);
				r.update(cost);
				m.put(k, r);
			}
		}
		return r;
	}

	/**
	 * 错误数
	 * 
	 */
	public Counter meterError(TopolLink topol) {
		Counter r = null;
		long count = topol.errorCount();
		if (topol != null && topol.errorCount() > 0) {
			String k = genMetricName(topol, ERROR);
			Map<String, Metric> m = metricSet.getMetrics();
			if (m.containsKey(k)) {
				r = ((Counter) m.get(k));
				r.inc(count);
			} else {
				r = metrics.counter(k);
				r.inc(count);
				m.put(k, r);
			}
		}
		return r;
	}

	@SuppressWarnings("unchecked")
	public <T> T getMetric(String k, Class<T> t) {
		Map<String, Metric> m = metricSet.getMetrics();
		if (m.containsKey(k)) {
			return ((T) m.get(k));
		} else {
			return null;
		}
	}

	public static String genMetricName(TopolLink topol, String metricType) {
		String s = null;
		if (QPS.equals(metricType)) {
			s = QPS;
		}
		if (ERROR.equals(metricType)) {
			s = ERROR;
		}
		if (COST.equals(metricType)) {
			s = COST;
		}
		if (topol != null) {
			s += topol.parent() + M + topol.child();
		}
		return s;
	}

	public Map<String, Set<Metric>> getAllMetrics() {
		Map<String, Set<Metric>> map = null;
		Map<String, Metric> metrics = metricSet.getMetrics();
		if (metrics != null) {
			map = new HashMap<>();
			for (Map.Entry<String, Metric> entry : metrics.entrySet()) {
				if (entry != null) {
					Set<Metric> sets = null;
					String key = entry.getKey();
					key = key.replaceFirst(MetricService.COST, "");
					key = key.replaceFirst(MetricService.ERROR, "");
					key = key.replaceFirst(MetricService.QPS, "");
					sets = map.get(key) != null ? map.get(key) : new HashSet<>();
					sets.add(entry.getValue());
					map.put(key, sets);
				}
			}
		}
		return map;
	}

}
