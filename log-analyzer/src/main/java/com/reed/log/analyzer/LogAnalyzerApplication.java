package com.reed.log.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LogAnalyzerApplication {

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(LogAnalyzerApplication.class, args);
		// 启动Reporter
		// ConsoleReporter reporter = ctx.getBean(ConsoleReporter.class);
		// reporter.start(5, TimeUnit.SECONDS);
	}

}
