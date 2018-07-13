package com.reed.log.zipkin.dependency;

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
public class DependencyAnalyzerApplication {
	public static Logger logger = LoggerFactory.getLogger(DependencyAnalyzerApplication.class);

	public static void main(String[] args) {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
		ApplicationContext ctx = SpringApplication.run(DependencyAnalyzerApplication.class, args);
		Environment environment = ctx.getEnvironment();

		logger.info("<==========LogAnalyzer started in port:{}===========>",
				environment.getProperty("local.server.port"));
	}

}
