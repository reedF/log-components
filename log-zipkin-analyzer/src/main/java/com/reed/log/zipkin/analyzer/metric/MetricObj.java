package com.reed.log.zipkin.analyzer.metric;

import com.reed.log.zipkin.analyzer.pojo.BaseObj;

public class MetricObj extends BaseObj {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5287361592731463645L;

	private long id;

	private long pid;

	private String appName;

	private String name;

	private Long total;

	private Double qps;

	private Double qpsMax;
	// 平均耗时，单位微秒（1秒= 1000 * 1000 微秒）
	private Double cost;

	private Double costTp99;

	private Double costTp95;
	// 平均耗时极值，单位微秒
	private Long costMax;
	// 错误数
	private long errors;
	// 错误率(%)
	private Double errorRate = (this.total != null && this.total > 0) ? this.errors / this.total.doubleValue() * 100.00
			: null;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getPid() {
		return pid;
	}

	public void setPid(long pid) {
		this.pid = pid;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getTotal() {
		return total;
	}

	public void setTotal(Long total) {
		this.total = total;
	}

	public Double getQps() {
		return qps;
	}

	public void setQps(Double qps) {
		this.qps = qps;
	}

	public Double getQpsMax() {
		return qpsMax;
	}

	public void setQpsMax(Double qpsMax) {
		this.qpsMax = qpsMax;
	}

	public Double getCost() {
		return cost;
	}

	public void setCost(Double cost) {
		this.cost = cost;
	}

	public Double getCostTp99() {
		return costTp99;
	}

	public void setCostTp99(Double costTp99) {
		this.costTp99 = costTp99;
	}

	public Double getCostTp95() {
		return costTp95;
	}

	public void setCostTp95(Double costTp95) {
		this.costTp95 = costTp95;
	}

	public Long getCostMax() {
		return costMax;
	}

	public void setCostMax(Long costMax) {
		this.costMax = costMax;
	}

	public long getErrors() {
		return errors;
	}

	public void setErrors(long errors) {
		this.errors = errors;
	}

	public Double getErrorRate() {
		return (this.total != null && this.total > 0) ? this.errors / this.total.doubleValue() * 100.000 : null;
	}

	public void setErrorRate(Double errorRate) {
		this.errorRate = errorRate;
	}

}
