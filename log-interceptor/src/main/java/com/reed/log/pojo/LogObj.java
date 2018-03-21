package com.reed.log.pojo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 日志对象
 *
 */
public class LogObj implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3094786054588370495L;
	// 请求地址
	private String uri = null; // 请求地址
	// http method
	private String method; 
	// http status
	private int httpCode; 
	// 传入参数，k:参数名，v:参数值json
	private Map<String, Object> inputParamMap = new HashMap<>(); 
	// 存放输出结果,K:数据类型,V:返回值json
	private Map<String, Object> outputParamMap = null; 
	// 开始时间(毫秒)
	private long startTime = 0; 
	// 结束时间(毫秒)
	private long endTime = 0; 
	private long cost = endTime - startTime;

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public int getHttpCode() {
		return httpCode;
	}

	public void setHttpCode(int httpCode) {
		this.httpCode = httpCode;
	}

	public Map<String, Object> getInputParamMap() {
		return inputParamMap;
	}

	public void setInputParamMap(Map<String, Object> inputParamMap) {
		this.inputParamMap = inputParamMap;
	}

	public Map<String, Object> getOutputParamMap() {
		return outputParamMap;
	}

	public void setOutputParamMap(Map<String, Object> outputParamMap) {
		this.outputParamMap = outputParamMap;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public long getCost() {
		return endTime - startTime;
	}

	public void setCost(long cost) {
		this.cost = cost;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

}
