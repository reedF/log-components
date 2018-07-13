package com.reed.log.zipkin.analyzer.es;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.reed.log.zipkin.analyzer.pojo.BaseObj;
import com.reed.log.zipkin.analyzer.pojo.ZipkinLog;

@Document(indexName = "zipkin-trace-#{new java.text.SimpleDateFormat('yyyy-MM-dd').format(new java.util.Date())}", type = "zipkin")
public class EsZipkin extends BaseObj {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6288661494011854218L;

	@Id
	private Long id = System.nanoTime();

	@Field(type = FieldType.keyword)
	private String app;

	// refer to EsTypeEnum
	@Field(type = FieldType.keyword)
	private String type;

	@Field(type = FieldType.keyword)
	private String traceId;

	@Field(type = FieldType.Object)
	// jeasyui-datagrid解析json格式要求
	@JsonProperty(value = "children")
	private ZipkinLog span;

	@Field(format = DateFormat.custom, store = true, type = FieldType.Date, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ", timezone = "GMT+8")
	private Date createTime;

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public ZipkinLog getSpan() {
		return span;
	}

	public void setSpan(ZipkinLog span) {
		this.span = span;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

}
