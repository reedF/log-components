package com.reed.log.analyzer.metric;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.metrics.ElasticsearchReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;

@Configuration
public class MetricConfig {

	public static Logger logger = LoggerFactory.getLogger(MetricConfig.class);
	// report存储在es的索引前缀
	public static final String esIndex_prefix = "metrics";
	// report上报间隔时间单位
	public static final TimeUnit unit = TimeUnit.MINUTES;//TimeUnit.MINUTES SECONDS;

	@Value("${es.hosts}")
	private String esHosts;
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
		r.start(reportInterval, unit);
		return r;
	}

	@Bean
	public ElasticsearchReporter esReporter(MetricRegistry metrics, SystemOutNotifier notifier) {
		ElasticsearchReporter r = null;
		try {
			r = ElasticsearchReporter.forRegistry(metrics)
					// support for several es nodes: "ip1:port","ip2:port"
					.hosts(esHosts)
					// just create an index, no date format, means one index
					// only
					.index(esIndex_prefix).indexDateFormat("yyyy-MM-dd")
					// define a percolation check on all metrics
					.percolationFilter(MetricFilter.ALL)
					// notifer
					// .percolationNotifier(notifier)
					.build();
			r.start(reportInterval, unit);
		} catch (IOException e) {
			logger.error("es reporter init failed>>>>>>>>>" + e.getMessage());
		}
		return r;
	}

	@Bean
	public JmxReporter jmxReporter(MetricRegistry metrics) {
		return JmxReporter.forRegistry(metrics).build();
	}

	@Bean
	public SystemOutNotifier esNotifier() {
		return new SystemOutNotifier();
	}

	@Bean
	public MetricSet metricSet() {
		MetricSet set = () -> new HashMap<>();
		return set;
	}

	/**
	 * TPS 计算器
	 *
	 * @param metrics
	 * @return
	 */
	// @Bean
	public Meter requestMeter(MetricRegistry metrics) {
		return metrics.meter("request");
	}

	/**
	 * 直方图
	 *
	 * @param metrics
	 * @return
	 */
	// @Bean
	public Histogram responseSizes(MetricRegistry metrics) {
		return metrics.histogram("response-sizes");
	}

	/**
	 * 计数器
	 *
	 * @param metrics
	 * @return
	 */
	// @Bean
	public Counter pendingJobs(MetricRegistry metrics) {
		return metrics.counter("requestCount");
	}

	/**
	 * 计时器
	 *
	 * @param metrics
	 * @return
	 */
	// @Bean
	public Timer responses(MetricRegistry metrics) {
		return metrics.timer("executeTime");
	}

}