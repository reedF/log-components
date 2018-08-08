package test.beam;

import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class KafkaUtils {

	public static void main(String[] args) {
		changeLogLevel();
		send(KafkaBeamTest.borkers, KafkaBeamTest.topic);
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		producer.close();
	}
	
	public static void changeLogLevel() {
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getLogger("org.apache.kafka").setLevel(Level.OFF);
	}
}
