package com.reed.log.kafka;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import com.reed.log.common.JobConfig;
import com.reed.log.common.RunnerTypeEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * get kafka settings
 * @author reed
 *
 */
@Slf4j
public class KafkaConfig {

	public static final String JAAS = "kafka_client_jaas.conf";

	public static Map<String, Object> getConsumerProperties() {
		Map<String, Object> map = new HashMap<>();
		// settings,
		// enable.auto.commit必须配true，否则不消费
		map.put("enable.auto.commit", "true");
		map.put("auto.offset.reset", "latest");
		map.put(ConsumerConfig.GROUP_ID_CONFIG, JobConfig.getKafkaGroup());
		setAuth(map);
		return map;
	}

	/**
	 * 配置kafka权限
	 * @param props
	 */
	public static void setAuth(Map<String, Object> map) {
		URL jaas = RunnerTypeEnum.Direct.equals(JobConfig.getRunner()) ? ClassLoader.getSystemResource(JAAS)
				: KafkaConfig.class.getClassLoader().getResource(JAAS);
		// according to jaas to set auth
		if (jaas != null) {
			// kafka auth
			System.setProperty("java.security.auth.login.config", jaas.toString());
			// configure the following three settings for SSL Encryption
			map.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
			map.put("sasl.mechanism", "PLAIN");
			log.info("=========load kafka jaas:{}========", jaas.toString());
		} else {
			log.info("=========Not find kafka jaas:{}========", jaas);
		}
	}
}
