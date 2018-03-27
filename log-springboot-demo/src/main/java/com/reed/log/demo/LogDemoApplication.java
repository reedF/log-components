package com.reed.log.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class LogDemoApplication {
	public static Logger logger = LoggerFactory.getLogger(LogDemoApplication.class);

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(LogDemoApplication.class, args);

		Environment environment = ctx.getEnvironment();
		logger.info("<==========Application started in port:{}===========>",
				environment.getProperty("local.server.port"));
	}

}
