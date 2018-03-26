package com.reed.log.analyzer.kafka;

/**
 * Logstash msg fields ex:
 * {"@timestamp":"2018-03-21T16:50:40.399+08:00","@version":1,"message":"{\"cost\":0,\"endTime\":1521622240399,\"httpCode\":200,\"inputParamMap\":{},\"method\":\"POST\",\"outputParamMap\":{\"LogObj\":{}","logger_name":"com.reed.log.interceptor.LogAspect","thread_name":"http-nio-8080-exec-3","level":"INFO","level_value":20000,"HOSTNAME":"USER-20170815QH","appname":"log-test"}
 * 
 * @author reed
 *
 */
public class MsgConstants {

	public static final String MSG = "message";

	public static final String URI = "uri";

	public static final String STATUS = "httpCode";

	public static final String OUT = "outputParamMap";

	public static final String CODE = "code";

}
