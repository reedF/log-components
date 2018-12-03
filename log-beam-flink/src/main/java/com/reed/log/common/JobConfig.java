package com.reed.log.common;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.PipelineRunner;
import org.apache.commons.lang.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * properties配置文件
 */
@Slf4j
public class JobConfig {
	public static final String PROPS_NAME = "job.properties";
	public static final String RUNNER_TYPE_DIRECT = "direct";
	public static final String RUNNER_TYPE_FLINK = "flink";

	public static final String KEY_RUNNING_ENV = "job.running.env";
	// kafka setting
	public static final String KEY_KAFKA_BROKERS = "job.kafka.brokers";
	public static final String KEY_KAFKA_TOPICS_INPUT = "job.kafka.topics.input";
	public static final String KEY_KAFKA_TOPICS_OUTPUT = "job.kafka.topics.output";
	public static final String KEY_KAFKA_GROUP = "job.kafka.group";
	// es
	public static final String KEY_ES_CLUSTER = "job.es.cluster";
	public static final String KEY_ES_INDEX_NAME = "job.es.index.name";

	public static Properties CONFIG = new Properties();

	static {
		// String path = JobConfig.class.getResource("/").getPath() +PROPS_NAME;
		//使用flink-runner时，不再是本地jvm启动的ClassLoader，而是flink内runner,无法使用ClassLoader.getSystemResourceAsStream加载
		//try (InputStream input = ClassLoader.getSystemResourceAsStream(PROPS_NAME)) {
		try (InputStream input = JobConfig.class.getClassLoader().getResourceAsStream(PROPS_NAME)) {
			if (input != null) {
				CONFIG.load(input);
				//log.info("========Properties setting runner is:{}=======", CONFIG.getProperty(KEY_RUNNING_ENV));
			}			
			log.info("========Job Properties:========");
			CONFIG.forEach((k, v) -> {
				log.info("-----{}={}-----", k, v);
			});
		} catch (Exception e) {
			log.error("===========Fail to load properties,ex is:{}==========", e.getMessage());
			e.printStackTrace();
		}
	}

	public static Class<PipelineRunner<PipelineResult>> getRunner() {
		return RunnerTypeEnum.getRunner(CONFIG.getProperty(KEY_RUNNING_ENV, RUNNER_TYPE_DIRECT));
	}

	public static String getKafkaBrokers() {
		return CONFIG.getProperty(KEY_KAFKA_BROKERS);
	}

	public static String getKafkaGroup() {
		return CONFIG.getProperty(KEY_KAFKA_GROUP);
	}

	public static List<String> getKafkaTopicsInput() {
		List<String> list = null;
		String str = CONFIG.getProperty(KEY_KAFKA_TOPICS_INPUT);
		if (StringUtils.isNotBlank(str)) {
			list = Arrays.asList(Arrays.stream(str.split(",")).map(String::trim).toArray(String[]::new));
		}
		return list;
	}

	public static String getKafkaTopicsOutput() {
		return CONFIG.getProperty(KEY_KAFKA_TOPICS_OUTPUT);
	}

	public static String[] getEsCluster() {
		String[] urls = null;
		String str = CONFIG.getProperty(KEY_ES_CLUSTER);
		if (StringUtils.isNotBlank(str)) {
			urls = Arrays.stream(str.split(",")).map(String::trim).toArray(String[]::new);
		}
		return urls;
	}

	public static String getEsIndexName() {
		return CONFIG.getProperty(KEY_ES_INDEX_NAME);
	}
}
