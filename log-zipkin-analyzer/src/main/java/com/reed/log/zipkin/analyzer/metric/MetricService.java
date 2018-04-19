package com.reed.log.zipkin.analyzer.metric;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.reed.log.zipkin.analyzer.pojo.TagsContents;
import com.reed.log.zipkin.analyzer.pojo.ZipkinLog;

@Service
public class MetricService {

	public static final String M = "||";
	public static final String ERROR = "ERROR-";
	public static final String METER_BIZ = "BIZ-CODE-";
	public static final String HISTOGRAM_COST = "COST-";
	public static final String SUCCESS = "200";

	@Autowired
	private MetricRegistry metrics;

	@Autowired
	private MetricSet metricSet;

	public void initMetricObj(ZipkinLog msg) {

		if (msg != null) {
			String app = msg.getLocalEndpoint() != null ? msg.getLocalEndpoint().getServiceName() : null;
			app = msg.getParentId() == null ? app : app + M + msg.getKind();
			String error = msg.getTags() != null ? msg.getTags().get(TagsContents.ERROR) : null;
			meterQps(app, msg.getName());
			histogramCost(app, msg.getName(), msg.getDuration());
			meterError(app, msg.getName(), error);
		}
	}

	/**
	 * 根据名称创建或更新监控
	 * 
	 * @param name
	 */
	public Meter meterQps(String appName, String uri) {
		String k = genMetricName(appName, uri, null);
		Meter meter = null;
		if (metricSet != null && metricSet.getMetrics() != null) {
			Map<String, Metric> m = metricSet.getMetrics();
			if (m.containsKey(k)) {
				meter = (Meter) m.get(k);
				meter.mark();
			} else {
				meter = metrics.meter(k);
				meter.mark();
				m.put(k, meter);
			}
		}
		return meter;
	}

	/**
	 * Http错误率
	 * 
	 * @param uri
	 * @param status
	 */
	public Counter meterError(String appName, String uri, String error) {
		Counter r = null;
		if (error != null && !error.equals(SUCCESS)) {
			String k = genMetricName(appName, uri, ERROR);
			Map<String, Metric> m = metricSet.getMetrics();
			if (m.containsKey(k)) {
				r = ((Counter) m.get(k));
				r.inc();
			} else {
				r = metrics.counter(k);
				r.inc();
				m.put(k, r);
			}
		}
		return r;
	}

	/**
	 * 请求耗时
	 * 
	 * @param uri
	 * @param cost
	 */
	public Histogram histogramCost(String appName, String uri, long cost) {
		Histogram r = null;
		if (metricSet != null && metricSet.getMetrics() != null) {
			String k = genMetricName(appName, uri, HISTOGRAM_COST);
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

	public <T> T getMetric(String k, Class<T> t) {
		Map<String, Metric> m = metricSet.getMetrics();
		if (m.containsKey(k)) {
			return ((T) m.get(k));
		} else {
			return null;
		}
	}

	public static String genMetricName(String appName, String uri, String type) {
		if (uri != null && uri.contains(M)) {
			String[] strs = uri.split("\\|\\|");
			appName = appName + M + strs[0];
			uri = strs[1];
		}
		return type == null ? (appName + M + uri) : (appName + M + type + uri);
	}

}
