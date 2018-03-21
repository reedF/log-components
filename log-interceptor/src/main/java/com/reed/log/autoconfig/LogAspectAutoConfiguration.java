package com.reed.log.autoconfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.reed.log.interceptor.LogAspect;

/**	
 */
@Configuration
@EnableConfigurationProperties(LogAspectProperties.class)
public class LogAspectAutoConfiguration {

	@Bean
	@ConditionalOnProperty(name = "log.aspect.enabled", havingValue = "true")
	public LogAspect logAspect() {
		return new LogAspect();
	}
}
