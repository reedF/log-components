package com.reed.log.test.kafka;

import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import kafka.message.Message;
/**
 * kafka自带工具：
 * 重置offset工具：
 * ./kafka-consumer-groups.bat --bootstrap-server 172.16.32.250:9092 --group test-dependency --reset-offsets --to-latest --all-topics --execute
 * 查询group：
 * ./kafka-consumer-groups.bat --bootstrap-server 172.16.32.250:9092 --describe --group apm-topol
 * @author reed
 *
 */
public class KafkaTool {  
  
	/**
	 * 重置group topic offset
	 * @param args: {"seek",$topicName,$partition,$newOffset}
	 * @return
	 */
    private static String seek(String []args){  
        String topic = args[2];  
        int partition = Integer.parseInt(args[3]);  
        int offset = Integer.parseInt(args[4]);  
          
        Properties props = new Properties();  
        props.put("bootstrap.servers", "192.168.59.103:9092");  
        props.put("group.id", "test");  
        props.put("enable.auto.commit", "true");  
        props.put("auto.commit.interval.ms", "1000");  
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, Message> consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);  
        //consumer.subscribe(Arrays.asList(topic)); //"deviceInfoTopic"  
        TopicPartition topicPartition = new TopicPartition(topic, partition);  
        consumer.assign(Arrays.asList(topicPartition));  
          
        consumer.seek(new TopicPartition(topic, partition), offset);  
        consumer.close();  
        return "SUCC";  
    }  
      
    public static void main(String[] args) {  
        System.out.println(args[1]);  
        if("seek".equals(args[1])){  
            System.out.println(seek(args));  
        }  
  
    }  
}  