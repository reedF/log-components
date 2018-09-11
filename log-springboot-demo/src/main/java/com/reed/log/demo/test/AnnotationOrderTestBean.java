package com.reed.log.demo.test;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 *测试：
 *1.@PostConstruct注解在bean初始化时的执行顺序
 *2.@PostConstruct与@ConditionalOnProperty共用时，是否有效
 * @author reed
 *
 */
@Component
@ConditionalOnProperty(prefix = "endpoints", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AnnotationOrderTestBean implements InitializingBean {

	// 测试config属性注入
	@Value("${endpoints.enabled}")
	public boolean tag;

	public AnnotationOrderTestBean() {
		System.out.println("InitSequenceBean: constructor");
	}

	/**
	 * @ConditionalOnProperty对@PostConstruct控制无效
	 */
	@ConditionalOnProperty(prefix = "endpoints", name = "enabled", havingValue = "false", matchIfMissing = true)
	@PostConstruct
	public void postConstruct() {
		System.out.println("InitSequenceBean: postConstruct" + "===" + tag);
	}

	public void initMethod() {
		System.out.println("InitSequenceBean: init-method");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("InitSequenceBean: afterPropertiesSet");
	}

	@Bean
	@ConditionalOnProperty(prefix = "endpoints", name = "enabled", havingValue = "true", matchIfMissing = true)
	public AnnotationOrderTestBean genBean() {
		System.out.println("InitSequenceBean: create new bean" + "===" + tag);
		return new AnnotationOrderTestBean();
	}
}