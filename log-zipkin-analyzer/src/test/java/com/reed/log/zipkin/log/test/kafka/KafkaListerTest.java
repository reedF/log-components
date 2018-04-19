package com.reed.log.zipkin.log.test.kafka;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 默认消费kafka topic全部partition,多个实例时，会在实例间自动分配partition消费
 */
@EnableAutoConfiguration
public class KafkaListerTest {

	public static Logger logger = LoggerFactory.getLogger(KafkaListerTest.class);

	@Autowired
	private KafkaTemplate<String, String> template;

	public static void main(String[] args) {
		SpringApplication.run(KafkaListerTest.class, args);
	}

	@KafkaListener(id = "test", topics = "zipkin-analyzer-counts-changelog")
	private void listen(ConsumerRecord<?, ?> cr) {
		logger.info("{} - {} : {}", cr.topic(), cr.key(), cr.value());
	}

}