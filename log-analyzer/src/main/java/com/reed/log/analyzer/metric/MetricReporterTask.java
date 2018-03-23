package com.reed.log.analyzer.metric;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;

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

	@Autowired
	private MetricRegistry metrics;
	@Autowired
	private MetricSet metricSet;

	//test,每分钟执行
	@Scheduled(cron = "0 0/10 * * * ?")
	public void timerForTest() {
		cleanMetrics();
	}

	// 每天23:59:56秒时执行
	@Scheduled(cron = scheduler)
	public void timerForProduction() {
		cleanMetrics();
	}

	private void cleanMetrics() {
		if (metricSet != null) {
			metricSet.getMetrics().clear();
		}
		metrics.removeMatching(MetricFilter.ALL);
		logger.info("Refresh Reporter current time : " + sdf.format(new Date()));
	}
}
