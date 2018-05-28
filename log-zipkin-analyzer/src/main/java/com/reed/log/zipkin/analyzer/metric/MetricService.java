package com.reed.log.zipkin.analyzer.metric;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.index.IndexNotFoundException;
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
import com.reed.log.zipkin.analyzer.es.EsTypeEnum;
import com.reed.log.zipkin.analyzer.es.EsZipkin;
import com.reed.log.zipkin.analyzer.es.EsZipkinRepository;
import com.reed.log.zipkin.analyzer.pojo.TagsContents;
import com.reed.log.zipkin.analyzer.pojo.ZipkinLog;

@Service
public class MetricService {

	public static final String M = "||";
	public static final String ERROR = "ERROR-";
	public static final String METER_BIZ = "BIZ-CODE-";
	public static final String HISTOGRAM_COST = "COST-";
	public static final String SUCCESS = "200";

	public static Logger logger = LoggerFactory.getLogger(MetricService.class);

	@Autowired
	private MetricRegistry metrics;

	@Autowired
	private MetricSet metricSet;

	@Autowired
	private EsZipkinRepository esZipkinRepository;

	public void initMetricObj(ZipkinLog msg) {

		if (msg != null) {
			String app = msg.getLocalEndpoint() != null ? msg.getLocalEndpoint().getServiceName() : null;
			app = msg.getParentId() == null ? app : app + M + msg.getKind();
			String error = msg.getTags() != null ? msg.getTags().get(TagsContents.ERROR) : null;
			meterQps(app, msg.getName());
			histogramCost(app, msg.getName(), msg.getDuration());
			meterError(app, msg.getName(), error);
			saveEsZipkin(msg);
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

	/**
	 * 保存错误、超长耗时的zipkin日志
	 * @param log
	 */
	public void saveEsZipkin(ZipkinLog log) {
		if (log != null) {
			String app = log.getLocalEndpoint() != null ? log.getLocalEndpoint().getServiceName() : null;
			app = log.getParentId() == null ? app : app + M + log.getKind();
			String error = log.getTags() != null ? log.getTags().get(TagsContents.ERROR) : null;
			Date time = new Date(log.getTimestamp() / 1000);
			// save error
			if (error != null && !error.equals(SUCCESS)) {
				EsZipkin es = new EsZipkin();
				es.setApp(app);
				es.setSpan(log);
				es.setTraceId(log.getTraceId());
				es.setType(EsTypeEnum.ERROR.getName());
				es.setCreateTime(time);
				doSave(es);
			}
			// save cost
			String cost = genMetricName(app, log.getName(), MetricService.HISTOGRAM_COST);
			Histogram h = getMetric(cost, Histogram.class);
			if (h != null) {
				double std = h.getSnapshot().getStdDev();
				if (log.getDuration() > h.getSnapshot().getMean() && std > 0) {
					double dev = (log.getDuration() - std) / std;
					if (dev > 0.7) {
						EsZipkin es = new EsZipkin();
						es.setApp(app);
						es.setSpan(log);
						es.setTraceId(log.getTraceId());
						es.setType(EsTypeEnum.COST.getName());
						es.setCreateTime(time);
						doSave(es);
					}
				}
			}
		}
	}

	private void doSave(EsZipkin es) {
		try {
			if (es != null) {
				List<EsZipkin> r = esZipkinRepository.findByTraceIdAndTypeAndApp(es.getTraceId(), es.getType(),
						es.getApp());
				if (r != null && !r.isEmpty()) {
					if (!r.stream().anyMatch(o -> o.getSpan().getId().equals(es.getSpan().getId()))) {
						esZipkinRepository.save(es);
					}
				} else {
					esZipkinRepository.save(es);
				}
			}
		} catch (IndexNotFoundException | SearchPhaseExecutionException e) {
			// 由于使用了findByTraceIdAndTypeAndApp在save前查询索引,会导致索引不存在时出现运行时异常，从而导致stream
			// task线程退出，故需处理异常，保证消费
			// handle runtime exception for index not exist
			logger.error("No Trace index:{},{},{}", e.getIndex(), e.getMessage(), e.getDetailedMessage());
			esZipkinRepository.index(es);
		} catch (Exception e) {
			logger.error("Save Trace ERROR:{},{}", e.getMessage(), e.getCause());
			esZipkinRepository.index(es);
		}
	}
}
