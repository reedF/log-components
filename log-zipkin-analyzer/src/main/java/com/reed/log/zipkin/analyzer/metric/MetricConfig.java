package com.reed.log.zipkin.analyzer.metric;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.metrics.ElasticsearchReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;

@Configuration
public class MetricConfig {

	public static Logger logger = LoggerFactory.getLogger(MetricConfig.class);
	// report存储在es的索引前缀
	public static final String esIndex_prefix = "zipkin-analyzer";
	// report上报间隔时间单位
	public static final TimeUnit unit = TimeUnit.MINUTES;// TimeUnit.MINUTES
															// SECONDS;

	@Value("${es.reporter.enable}")
	private boolean sendReport;
	@Value("${es.hosts}")
	private String[] esHosts;
	@Value("${es.reorter.interval}")
	private int reportInterval;

	@Bean
	public MetricRegistry metrics(MetricSet sets) {
		MetricRegistry m = new MetricRegistry();
		m.registerAll(sets);
		return m;
	}

	/**
	 * Reporter 数据的展现位置
	 *
	 * @param metrics
	 * @return
	 */
	@Bean
	public ConsoleReporter consoleReporter(MetricRegistry metrics) {
		ConsoleReporter r = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).build();
		// r.start(reportInterval, unit);
		return r;
	}

	@Bean
	public ElasticsearchReporter esReporter(MetricRegistry metrics) {
		ElasticsearchReporter r = null;
		if (sendReport) {
			try {
				Map<String, Object> fields = new HashMap<>();
				// add new fields to es index
				fields.put("host", getIp());
				r = ElasticsearchReporter.forRegistry(metrics)
						// support for several es nodes: "ip1:port","ip2:port"
						.hosts(esHosts)
						// create an index, no date format, means just one index
						.index(esIndex_prefix).indexDateFormat("yyyy-MM-dd")
						// add customer fields
						.additionalFields(fields)
						// define a percolation check on all metrics
						.percolationFilter(MetricFilter.ALL).build();
				r.start(reportInterval, unit);
			} catch (IOException e) {
				logger.error("es reporter init failed>>>>>>>>>" + e.getMessage());
			}
		}
		return r;
	}

	@Bean
	public MetricSet metricSet() {
		Map<String, Metric> map = new ConcurrentHashMap<>();
		MetricSet set = () -> map;
		return set;
	}

	private String getIp() {
		String host = null;
		try {
			host = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			logger.error("get server host Exception e:>>>>>>>>>", e);
		}
		return host;
	}

}