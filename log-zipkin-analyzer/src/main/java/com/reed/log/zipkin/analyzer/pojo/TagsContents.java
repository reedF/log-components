package com.reed.log.zipkin.analyzer.pojo;

public class TagsContents {

	public static final String SQL = "sql.query";

	public static final String GRPC = "grpc.request";

	public static final String HTTP_PATH = "http.path";

	public static final String HTTP_URL = "http.url";
	//与HTTP_URL匹配出现
	public static final String HTTP_STATUS = "http.status_code";
	//与HTTP_URL匹配出现
	public static final String REQ = "requestID";

	public static final String CLASS = "class";
	////与CLASS匹配出现
	public static final String PARAM = "param";

	public static final String ERROR = "error";
}
