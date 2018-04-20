/**
  * Copyright 2018 bejson.com 
  */
package com.reed.log.zipkin.analyzer.pojo;

import java.util.Map;

/**
 * Zipkin logs
 * refer to : https://zipkin.io/zipkin-api/#/default/post_spans
 */
public class ZipkinLog extends BaseObj {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2160352570939059539L;

	private String traceId;
	private String id;
	private String parentId;
	/**
	 * CLIENT 
	 * timestamp - The moment a request was sent (formerly “cs”) 
	 * duration  - When present indicates when a response was received (formerly “cr”)
	 * remoteEndpoint - Represents the server. Leave serviceName absent if  unknown. 
	 * SERVER 
	 * timestamp - The moment a request was received (formerly “sr”) 
	 * duration - When present indicates when a response was sent (formerly “ss”) 
	 * remoteEndpoint - Represents the client. Leave serviceName absent if unknown. 
	 * PRODUCER 
	 * timestamp - The moment a message was sent to a destination (formerly “ms”) 
	 * duration - When present represents delay sending the message, such as batching. 
	 * remoteEndpoint - Represents the broker. Leave serviceName absent if unknown. 
	 * CONSUMER 
	 * timestamp - The moment a message was received from an origin (formerly “mr”) 
	 * duration - When present represents delay consuming the message, such as from backlog. 
	 * remoteEndpoint - Represents the broker. Leave serviceName absent if unknown.
	 */
	private String kind;
	private String name;
	private long timestamp;
	// 开始span到结束span的时间,单位微秒
	private long duration;
	private boolean shared;
	private Endpoint localEndpoint;
	private Endpoint remoteEndpoint;
	private Map<String, String> tags;

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getKind() {
		return kind;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public long getDuration() {
		return duration;
	}

	public void setLocalEndpoint(Endpoint localEndpoint) {
		this.localEndpoint = localEndpoint;
	}

	public Endpoint getLocalEndpoint() {
		return localEndpoint;
	}

	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	public Endpoint getRemoteEndpoint() {
		return remoteEndpoint;
	}

	public void setRemoteEndpoint(Endpoint remoteEndpoint) {
		this.remoteEndpoint = remoteEndpoint;
	}

	public Map<String, String> getTags() {
		return tags;
	}

	public void setTags(Map<String, String> tags) {
		this.tags = tags;
	}

}