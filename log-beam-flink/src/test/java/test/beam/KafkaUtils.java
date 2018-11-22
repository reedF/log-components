package test.beam;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;

public class KafkaUtils {

	public static final String borkers = "192.168.59.103:9092";// "172.16.32.86:9092";//"192.168.59.103:9092";
	public static final String topic = "logs";// "bd_canal_pos";//"db_order_new";
	public static final String topic_result = "results";
	public static final String group = "test";

	public static void main(String[] args) {
		changeLogLevel();
		//send(borkers, topic);
		//listen(topic, false);
		listen(topic_result, false);
	}
	

	public static void send(String borkerList, String topic) {
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
		for (int i = 0; i <= 1000; i++) {
			String key = UUID.randomUUID().toString();
			String v = key + "-" + i + "-" + System.currentTimeMillis();
			producer.send(new ProducerRecord<String, String>(topic, key, v));
			System.out.println("=======" + v);
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		producer.close();
	}

	public static void listen(String topics, boolean isOffsetByManual) {

		Properties props = new Properties();
		props.put("bootstrap.servers", borkers);
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
							System.out.printf("offset by manual = %d, key = %s, value = %s%n", record.offset(),
									record.key(), record.value());
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

	public static void changeLogLevel() {
		// Logger.getRootLogger().setLevel(Level.INFO);
		// Logger.getLogger("org.apache.kafka").setLevel(Level.OFF);
	}
}
