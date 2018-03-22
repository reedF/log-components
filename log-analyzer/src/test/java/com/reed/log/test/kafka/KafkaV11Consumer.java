package com.reed.log.test.kafka;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;

/**
 * Kafka version >= 0.11.0时，新版生产消费API
 * 
 * @author reed
 *
 */
public class KafkaV11Consumer {

	public void send(String borkerList, String topic) {
		Properties props = new Properties();
		props.put("bootstrap.servers", borkerList);
		props.put("acks", "all");
		props.put("retries", 0);
		props.put("batch.size", 16384);
		props.put("linger.ms", 1);
		props.put("buffer.memory", 33554432);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		Producer<String, String> producer = new KafkaProducer<>(props);
		boolean flag = true;
		Scanner input = new Scanner(System.in);
		do {
			System.out.println("请输入key和value的格式");
			producer.send(new ProducerRecord<String, String>(topic, input.nextLine(), input.nextLine()));
		} while (flag);
		producer.close();
	}

	public void get(String borkerList, String topics, String group, boolean isOffsetByManual) {
		Properties props = new Properties();
		props.put("bootstrap.servers", borkerList);
		props.put("group.id", group);
		props.put("enable.auto.commit", "true");
		props.put("auto.commit.interval.ms", "1000");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Arrays.asList(topics));
		boolean flag = true;

		try {
			while (flag) {
				if (!isOffsetByManual) {
					ConsumerRecords<String, String> records = consumer.poll(100);
					for (ConsumerRecord<String, String> record : records)
						System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(),
								record.value());
				} else {
					ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
					for (TopicPartition partition : records.partitions()) {
						List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
						for (ConsumerRecord<String, String> record : partitionRecords) {
							System.out.printf("offset by manual = %d, key = %s, value = %s%n", record.offset(), record.key(),
									record.value());
						}
						long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
						consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(lastOffset + 1)));
					}
				}
			}
		} finally {
			consumer.close();
		}

	}

	public static void main(String[] args) {
		String borkerList = "192.168.59.103:9092";
		String groupId = "test";
		String topic = "logs";
		KafkaV11Consumer p = new KafkaV11Consumer();
		// p.send(borkerList, topic);
		p.get(borkerList, topic, groupId, true);
	}
}
