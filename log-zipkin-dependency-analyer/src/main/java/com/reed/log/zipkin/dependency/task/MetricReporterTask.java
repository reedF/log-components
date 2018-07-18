package com.reed.log.zipkin.dependency.task;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.reed.log.zipkin.dependency.es.EsTopolResultService;

/**
 * 刷新统计报告，重置count
 * 
 * @author reed
 *
 */
@Component
public class MetricReporterTask {
	private static Logger logger = LoggerFactory.getLogger(MetricReporterTask.class);

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Autowired
	private MetricRegistry metrics;
	@Autowired
	private MetricSet metricSet;
	@Autowired
	private EsTopolResultService esResultService;

	@Bean(name = "taskScheduler")
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(5);
		return taskScheduler;
	}

	// send to es,每分钟执行
	// @Scheduled(cron = "0 0/1 * * * ?")
	@Scheduled(cron = "${metric.result.send.schedule}")
	public void timerForEs() {
		esResultService.saveAllCurrentResult();
		cleanMetrics();
		logger.info("=========Send Metric Result to ES current time : {}=========", sdf.format(new Date()));
	}

	private void cleanMetrics() {
		if (metricSet != null) {
			metricSet.getMetrics().clear();
		}
		metrics.removeMatching(MetricFilter.ALL);
		//logger.info("=========Refresh Reporter current time : {} =========", sdf.format(new Date()));
	}

}
