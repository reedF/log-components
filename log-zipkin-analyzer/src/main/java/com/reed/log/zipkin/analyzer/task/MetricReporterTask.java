package com.reed.log.zipkin.analyzer.task;

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
import com.reed.log.zipkin.analyzer.es.EsMetricResultService;

/**
 * 刷新统计报告，每天零点重置count
 * 
 * @author reed
 *
 */
@Component
public class MetricReporterTask {
	private static Logger logger = LoggerFactory.getLogger(MetricReporterTask.class);

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	// 定时任务时间点
	private static final String scheduler = "56 59 23 * * ?";
	private static final String scheduler_es = "01 57 23 * * ?";
	@Autowired
	private MetricRegistry metrics;
	@Autowired
	private MetricSet metricSet;
	@Autowired
	private EsMetricResultService esResultService;

	/**
	 * 自定义的任务线程池,避免默认单线程执行任务导致的任务无法执行问题
	 * @return
	 */
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
		int r = esResultService.saveAllCurrentResult();
		logger.info("=========Send Metric Result to ES current time : {},Data size : {}=========",
				sdf.format(new Date()), r);
	}

	// 每天23:59:56秒时执行
	@Scheduled(cron = scheduler)
	public void timerForReporter() {
		cleanMetrics();
	}

	// 每天23:57:01秒时执行,最后回收一次数据
	@Scheduled(cron = scheduler_es)
	public void timerForEnd() {
		timerForEs();
	}

	private void cleanMetrics() {
		if (metricSet != null) {
			metricSet.getMetrics().clear();
		}
		metrics.removeMatching(MetricFilter.ALL);
		logger.info("=========Refresh Reporter current time : {} =========", sdf.format(new Date()));
	}

}
