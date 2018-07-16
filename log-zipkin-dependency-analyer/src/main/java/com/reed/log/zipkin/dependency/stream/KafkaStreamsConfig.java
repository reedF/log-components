package com.reed.log.zipkin.dependency.stream;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;

import com.alibaba.fastjson.JSON;

import scala.Tuple2;

/**
 * Kafak stream config
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
	@Value("${spring.kafka.consumer.group-id}")
	private String groupId;
	@Value("${kafka.topic}")
	private String topic;
	@Value("${kafka.consumer.num}")
	private int consumerNum = 2;

	public static final String storesName = "counts";

	public static final String M = "||";

	// 统计时间窗口长度，毫秒
	public static final int windowSize = 10000;

	public static Logger logger = LoggerFactory.getLogger(KafkaStreamsConfig.class);

	@Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
	public StreamsConfig kStreamsConfigs() {
		Map<String, Object> props = new HashMap<>();
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, groupId);
		props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, consumerNum);
		props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		// 可控制KStream向topic发送数据的时间频率，默认30s
		props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, windowSize);
		// cunsumer setting
		// 注：某些ConsumerConfig对kafka-stream启动的RestoreConsumer（消费store类topic）的消费者配置无效，参见{@link
		// #StreamsConfig.getRestoreConsumerConfigs}
		// props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
		// props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 2000);
		// props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,20000);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		// producer setting
		props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1024 * 1024 * 20);
		// props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
		// new JsonSerde<TreeObj>(TreeObj.class).getClass().getName());
		props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class.getName());

		return new StreamsConfig(props);
	}

	/**
	 * 测试处理统计zipkin原生DependencyLink（依赖关系）数据
	 * 使用kafkastream-1.0版本，部分API迁移到新版
	 * @param kStreamBuilder
	 * @param aggregator
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "deprecation" })
	//@Bean
	public KStream<String, String> kStreamV1TestZipkinDepdencyLink(StreamsBuilder kStreamBuilder) {
		// StateStoreSupplier myStore =
		// Stores.create(storesName).withStringKeys().withStringValues().inMemory().build();
		StoreBuilder builder = Stores.keyValueStoreBuilder(Stores.inMemoryKeyValueStore(storesName), Serdes.String(),
				Serdes.Bytes());
		kStreamBuilder.addStateStore(builder.withLoggingDisabled());
		KStream<String, String> stream = kStreamBuilder.stream(topic);

		KStream<String, String> out = stream
				// [(k,v),(k,v),(k,v)...] to DependencyLink(k,v) key is traceId
				.transform(() -> new DependencyTransformer(), storesName)
				// change key
				// .selectKey((k, v) -> v.getApp() + M + v.getName())
				// .groupByKey(Serialized.with(Serdes.String(), new
				// JsonSerde<>(TreeObj.class)))
				// .aggregate(() -> null, aggregator, new
				// JsonSerde<>(MetricObj.class), storesName + "-agg")
				// .peek((k, v) -> aggregator.apply(null, v, null))
				// .toStream()
				.mapValues(v -> v.toString())
		//
		;

		out.print();
		return stream;
	}
	
	
	@SuppressWarnings("rawtypes")
	@Bean
	public KStream<String, String> kStreamV1(StreamsBuilder kStreamBuilder, MetricAggregator aggregator) {
		StoreBuilder builder = Stores.keyValueStoreBuilder(Stores.inMemoryKeyValueStore(storesName), Serdes.String(),
				Serdes.Bytes());
		kStreamBuilder.addStateStore(builder.withLoggingDisabled());
		KStream<String, String> stream = kStreamBuilder.stream(topic);

		KStream<String, String> out = stream
				// [(k,v),(k,v),(k,v)...] to DependencyLink(k,v) key is traceId
				.transform(() -> new TopolLinkTransformer(), storesName)
				// change key
				// .selectKey((k, v) -> v.getApp() + M + v.getName())
				// .groupByKey(Serialized.with(Serdes.String(), new
				// JsonSerde<>(TreeObj.class)))
				// .aggregate(() -> null, aggregator, new
				// JsonSerde<>(MetricObj.class), storesName + "-agg")
				// .peek((k, v) -> aggregator.apply(null, v, null))
				// .toStream()
				.mapValues(v -> v.toString())
		//
		;

		out.print();
		return stream;
	}

	@Bean
	public MetricAggregator metricAggregator() {
		return new MetricAggregator();
	}

}