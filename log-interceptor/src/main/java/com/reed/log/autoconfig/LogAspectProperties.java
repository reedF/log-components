package com.reed.log.autoconfig;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * config for log aspect to pointcut
 */
@ConfigurationProperties("log.aspect")
public class LogAspectProperties {
	// 是否开启AOP拦截
	private boolean enabled = false;
	// 是否记录请求的返回结果
	private boolean canLogResult = false;
	// 要拦截的package,多个使用“,”分割
	private Set<String> packages;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Set<String> getPackages() {
		return packages;
	}

	public void setPackages(Set<String> packages) {
		this.packages = packages;
	}

	public boolean isCanLogResult() {
		return canLogResult;
	}

	public void setCanLogResult(boolean canLogResult) {
		this.canLogResult = canLogResult;
	}

}
