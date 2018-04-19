package com.reed.log.zipkin.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = { PersistenceExceptionTranslationAutoConfiguration.class })
@EnableElasticsearchRepositories
public class AnalyzerApplication {
	public static Logger logger = LoggerFactory.getLogger(AnalyzerApplication.class);

	public static void main(String[] args) {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
		ApplicationContext ctx = SpringApplication.run(AnalyzerApplication.class, args);
		Environment environment = ctx.getEnvironment();

		logger.info("<==========LogAnalyzer started in port:{}===========>",
				environment.getProperty("local.server.port"));
	}

}
