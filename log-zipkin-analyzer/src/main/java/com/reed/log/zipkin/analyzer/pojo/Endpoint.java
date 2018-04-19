package com.reed.log.zipkin.analyzer.pojo;

public class Endpoint extends BaseObj {

	private String ipv4;
	private int port;
	private String serviceName;

	public void setIpv4(String ipv4) {
		this.ipv4 = ipv4;
	}

	public String getIpv4() {
		return ipv4;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

}