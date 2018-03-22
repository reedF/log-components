package com.reed.log.analyzer.kafka;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 默认消费kafka topic全部partition,多个实例时，会在实例间自动分配partition消费
 */
public class KafkaLogListener {

	public static Logger logger = LoggerFactory.getLogger(KafkaLogListener.class);

	@Autowired
	private KafkaTemplate<String, String> template;

	public void send(String topic, String key, String data) {
		template.send(topic, key, data);
	}

	@KafkaListener(id = "test", topics = "logs")
	private void listen(ConsumerRecord<?, ?> cr) {
		logger.info("{} - {} : {}", cr.topic(), cr.key(), cr.value());
	}

	@KafkaListener(topics = "logs", containerFactory = "batchFactory")
	public void listenBatch(List<ConsumerRecord<?, ?>> list) {
		list.forEach(s -> {
			logger.info("batch======>{}", s.toString());
		});
	}
}