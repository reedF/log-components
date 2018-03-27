package com.reed.log.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LogAnalyzerApplication {
	public static Logger logger = LoggerFactory.getLogger(LogAnalyzerApplication.class);

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(LogAnalyzerApplication.class, args);
		Environment environment = ctx.getEnvironment();
		logger.info("<==========LogAnalyzer started in port:{}===========>",
				environment.getProperty("local.server.port"));
	}

}
