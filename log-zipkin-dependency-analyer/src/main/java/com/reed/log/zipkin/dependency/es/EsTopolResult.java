package com.reed.log.zipkin.dependency.es;

import java.util.Date;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonFormat;

@Document(indexName = "apm-topol-#{new java.text.SimpleDateFormat('yyyy-MM-dd').format(new java.util.Date())}", type = "topol")
public class EsTopolResult extends BaseObj {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6288661494011854218L;

	@Id
	private String id = UUID.randomUUID().toString();

	private String parentApp;

	private String parentUrl;

	private String childApp;

	private String childUrl;

	private String type;

	private Long count;

	private Double qps;

	private Double cost;

	private Long errors = 0l;

	// 错误率(%)
	private Double errorRate = (this.count != null && this.count > 0) ? this.errors / this.count.doubleValue() * 100.00
			: null;

	@Field(format = DateFormat.custom, store = true, type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ", timezone = "GMT+8")
	private Date createTime = new Date();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getParentApp() {
		return parentApp;
	}

	public void setParentApp(String parentApp) {
		this.parentApp = parentApp;
	}

	public String getParentUrl() {
		return parentUrl;
	}

	public void setParentUrl(String parentUrl) {
		this.parentUrl = parentUrl;
	}

	public String getChildApp() {
		return childApp;
	}

	public void setChildApp(String childApp) {
		this.childApp = childApp;
	}

	public String getChildUrl() {
		return childUrl;
	}

	public void setChildUrl(String childUrl) {
		this.childUrl = childUrl;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

	public Double getQps() {
		return qps;
	}

	public void setQps(Double qps) {
		this.qps = qps;
	}

	public Double getCost() {
		return cost;
	}

	public void setCost(Double cost) {
		this.cost = cost;
	}

	public Long getErrors() {
		return errors;
	}

	public void setErrors(Long errors) {
		this.errors = errors;
	}

	public Double getErrorRate() {
		return (this.count != null && this.count > 0) ? this.errors / this.count.doubleValue() * 100.00 : 0;
	}

	public void setErrorRate(Double errorRate) {
		this.errorRate = errorRate;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

}
