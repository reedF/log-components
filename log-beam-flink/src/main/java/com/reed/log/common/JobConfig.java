package com.reed.log.common;

import java.io.FileInputStream;
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
	public static final String PROPS_PATH = JobConfig.class.getResource("/").getPath() + "job.properties";
	public static final String RUNNER_TYPE_DIRECT = "direct";
	public static final String RUNNER_TYPE_FLINK = "flink";

	public static final String KEY_RUNNING_ENV = "job.running.env";
	// kafka setting
	public static final String KEY_KAFKA_BROKERS = "job.kafka.brokers";
	public static final String KEY_KAFKA_TOPICS_INPUT = "job.kafka.topics.input";
	public static final String KEY_KAFKA_TOPICS_OUTPUT = "job.kafka.topics.input";
	public static final String KEY_KAFKA_GROUP = "job.kafka.group";

	public static Properties CONFIG = new Properties();

	static {
		try (InputStream input = new FileInputStream(PROPS_PATH)) {
			if (input != null) {
				CONFIG.load(input);
				log.info("========Properties setting runner is:{}=======", CONFIG.getProperty(KEY_RUNNING_ENV));
			}
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

	public static List<String> getKafkaTopicsOutput() {
		List<String> list = null;
		String str = CONFIG.getProperty(KEY_KAFKA_TOPICS_OUTPUT);
		if (StringUtils.isNotBlank(str)) {
			list = Arrays.asList(Arrays.stream(str.split(",")).map(String::trim).toArray(String[]::new));
		}
		return list;
	}
}
