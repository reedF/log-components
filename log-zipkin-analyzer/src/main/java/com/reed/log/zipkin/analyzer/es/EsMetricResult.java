package com.reed.log.zipkin.analyzer.es;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.reed.log.zipkin.analyzer.metric.MetricObj;
import com.reed.log.zipkin.analyzer.pojo.BaseObj;

@Document(indexName = "metrics-zipkin-#{new java.text.SimpleDateFormat('yyyy-MM-dd').format(new java.util.Date())}", type = "metric")
public class EsMetricResult extends BaseObj {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6288661494011854218L;

	@Id
	private Long id = System.nanoTime();

	private String app;

	private String name;

	@Field(type = FieldType.Object)
	// jeasyui-datagrid解析json格式要求
	@JsonProperty(value = "children")
	private List<MetricObj> spans;

	//@Field(format = DateFormat.date_time, store = true, type = FieldType.Date,pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
	//@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ", timezone = "GMT+8")
	@Field(format = DateFormat.custom, store = true, type = FieldType.Date, pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime = new Date();
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getApp() {
		return app;
	}

	public void setApp(String app) {
		this.app = app;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<MetricObj> getSpans() {
		return spans;
	}

	public void setSpans(List<MetricObj> spans) {
		this.spans = spans;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

}
