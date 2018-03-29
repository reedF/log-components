package com.reed.log.analyzer.kafka.stream;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;
import org.apache.kafka.streams.state.Stores;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;

import com.reed.log.analyzer.kafka.MsgConstants;
import com.reed.log.analyzer.kafka.MsgUtil;

/**
 * Kafak stream config and demo to count
 * 
 * @author reed
 *
 */
@Configuration
@EnableKafka
@EnableKafkaStreams
public class KafkaStreamsConfig {
	@Value("${spring.kafka.bootstrap-servers}")
	private String brokerList;
	// @Value("${spring.kafka.consumer.group-id}")
	private String groupId = "kafka-streams";
	@Value("${kafka.topic}")
	private String topic;
	@Value("${kafka.consumer.num}")
	private int consumerNum = 2;

	public static final String stores = "counts";
	// 统计时间窗口长度，毫秒
	public static final int windowSize = 10000;

	@Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
	public StreamsConfig kStreamsConfigs() {
		Map<String, Object> props = new HashMap<>();
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, groupId);
		props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, consumerNum);
		props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class.getName());
		// 可控制KStream向topic发送数据的时间频率，默认30s
		props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, windowSize);
		// 消息处理方式：仅此一次，至少一次，默认StreamsConfig.AT_LEAST_ONCE
		// props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG,
		// StreamsConfig.EXACTLY_ONCE);
		return new StreamsConfig(props);
	}

	@SuppressWarnings("unchecked")
	@Bean
	public KStream<String, String> kStream(KStreamBuilder kStreamBuilder) {

		// kStreamBuilder.addSource("SOURCE", topic);
		// kStreamBuilder.addProcessor("PROCESSOR1", new
		// UriCountProcessorSupplier(), "SOURCE");
		// kStreamBuilder.addStateStore(Stores.create(stores).withStringKeys().withIntegerValues().inMemory().build(),null);
		// kStreamBuilder.addSink("SINK", "stream-out", "PROCESSOR1");

		KStream<String, String> stream = kStreamBuilder.stream(topic);
		// stream.process(new UriCountProcessorSupplier(), stores);

		stream.map(new KeyValueMapper<String, String, KeyValue<String, String>>() {
			public KeyValue<String, String> apply(String key, String v) {
				Map<String, Object> msg = MsgUtil.msg2Map(v);
				String appName = MsgUtil.getAppName(msg);
				Map<String, Object> data = MsgUtil.getData(msg);
				String uri = MsgUtil.getFieldValue(data, MsgConstants.URI);
				String k = appName + "-" + uri;
				return new KeyValue<String, String>(k, v);
			}
		}).groupByKey()
				// 简单count
				// .count(stores).toStream()
				// 窗口方式统计
				.count(TimeWindows.of(TimeUnit.SECONDS.toSeconds(windowSize)), stores).toStream()
				.map((windowedId, value) -> new KeyValue<>(windowedId.key(), value))
				// 自定义定制序列化及count value存储方式
				// .count(Stores.create(stores).withStringKeys().withLongValues().inMemory().build()).toStream()
				// 由于@KafkaListener消费时使用String反序列化，因此需对value从Long转换为String，以防消费时乱码
				// .mapValues(new ValueMapper<Long, String>() {
				// @Override
				// public String apply(Long value) {
				// return String.valueOf(value);
				// }
				// })
				// 与上方的mapValues()实现方式等效
				.mapValues(v -> String.valueOf(v))
				// send to new topic
				.to(Serdes.String(), Serdes.String(), stores);

		// stream.print();
		return stream;
	}

}