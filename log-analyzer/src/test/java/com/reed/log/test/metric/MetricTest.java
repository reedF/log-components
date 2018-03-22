package com.reed.log.test.metric;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.metrics.ElasticsearchReporter;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class MetricTest {
	static final MetricRegistry metrics = new MetricRegistry();

	public static void main(String[] args) {

		startReport();
		startEsReport();
		testMeter();
		testQps();
		testCost();
	}

	private static void startReport() {
		// 注册metrics,每个1秒打印metrics到控制台
		ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).build();
		reporter.start(5, TimeUnit.SECONDS);
	}

	private static void startEsReport() {
		try {
			ElasticsearchReporter reporter = ElasticsearchReporter.forRegistry(metrics)
					// support for several es nodes: "ip1:port","ip2:port"
					.hosts("192.168.59.103:9200")
					// just create an index, no date format, means one index
					// only
					.index("metrics").indexDateFormat("yyyy-MM-dd")
					// define a percolation check on all metrics
					.percolationFilter(MetricFilter.ALL).
					// notifer
					percolationNotifier(new SystemOutNotifier())
					// .percolationNotifier(new HttpNotifier())
					.build();
			// usually you set this to one minute
			reporter.start(10, TimeUnit.SECONDS);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void waitSeconds(int t) {
		try {
			Thread.sleep(t);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void testMeter() {
		// metrics:事件总数，平均速率,包含1分钟，5分钟，15分钟的速率
		Meter requests = metrics.meter("requests");
		// 计数一次
		requests.mark();
		waitSeconds(5000);
	}

	private static void testQps() {
		Timer timer = metrics.timer(MetricRegistry.name(MetricTest.class, "calculation-duration"));
		Random rn = new Random();
		while (true) {
			// 统计开始
			final Timer.Context context = timer.time();
			int sleepTime = rn.nextInt(2000);
			waitSeconds(sleepTime);
			System.out.println("处理耗时:" + sleepTime);
			// 统计结束
			context.stop();
		}
	}

	private static void testCost() {
		Histogram cost = metrics.histogram("cost");
		Random rn = new Random();
		while (true) {
			// 统计开始
			int sleepTime = rn.nextInt(2000);
			waitSeconds(sleepTime);
			cost.update(sleepTime);
		}

	}

}