package com.reed.log.zipkin.analyzer.stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology.AutoOffsetReset;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Serialized;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.processor.StateStoreSupplier;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.StoreSupplier;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.support.serializer.JsonSerde;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.reed.log.zipkin.analyzer.metric.MetricObj;
import com.reed.log.zipkin.analyzer.metric.MetricService;
import com.reed.log.zipkin.analyzer.pojo.TagsContents;
import com.reed.log.zipkin.analyzer.pojo.ZipkinLog;
import com.reed.log.zipkin.analyzer.tree.TreeObj;

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

	@Autowired
	public MetricService metricService;

	@Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
	public StreamsConfig kStreamsConfigs() {
		Map<String, Object> props = new HashMap<>();
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, groupId);
		props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, consumerNum);
		props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
		props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, windowSize);
		// cunsumer setting
		// 注：某些ConsumerConfig对kafka-stream启动的RestoreConsumer（消费store类topic）的消费者配置无效，参见{@link #StreamsConfig.getRestoreConsumerConfigs}
		//props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
		//props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 2000);
		//props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,20000);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		// producer setting
		props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1024 * 1024 * 20);
		// props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
		// new JsonSerde<TreeObj>(TreeObj.class).getClass().getName());
		props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class.getName());

		return new StreamsConfig(props);
	}

	// @Bean
	public KStream<String, String> kStreamTest(KStreamBuilder kStreamBuilder) {

		KStream<String, String> stream = kStreamBuilder.stream(topic);

		KStream<String, String> out = stream
				// [(k,v),(k,v),(k,v)...] to 集合(k,v) key is traceId
				.flatMap(new ZipkinKeyValueMapper())
				// 过滤无效数据
				.filterNot((k, v) -> (v.getTags() == null || v.getTags().containsKey(TagsContents.SQL)))
				// 重新映射
				.mapValues(new ValueMapper<ZipkinLog, TreeObj>() {
					@Override
					public TreeObj apply(ZipkinLog v) {
						TreeObj t = new TreeObj();
						t.setId(v.getId());
						t.setName(v.getLocalEndpoint() != null
								? v.getLocalEndpoint().getServiceName() + "-" + v.getName() : v.getName());
						t.setParentId(v.getParentId());
						return t;
					}
				})
				// group by traceId
				.groupByKey(Serdes.String(), new JsonSerde<>(TreeObj.class))
				// 合并同类trace,构造跟踪树
				.aggregate(() -> null, new Aggregator<String, TreeObj, TreeObj>() {
					@Override
					public TreeObj apply(String key, TreeObj value, TreeObj aggregate) {
						aggregate = value;
						return aggregate;
					}

				}, new JsonSerde<>(TreeObj.class), storesName)
				// table重新转换为stream
				.toStream()
				// 窗口方式统计
				// .count().toStream()
				.mapValues(v -> JSON.toJSONString(v));
		;

		out.print();
		return stream;
	}

	@SuppressWarnings("unchecked")
	// @Bean
	public KStream<String, String> kStreamTest2(KStreamBuilder kStreamBuilder) {

		KStream<String, String> stream = kStreamBuilder.stream(topic);

		KStream<String, String> out = stream
				// [(k,v),(k,v),(k,v)...] to 集合(k,v) key is traceId
				.flatMap(new ZipkinKeyValueMapper())
				// 过滤无效数据
				.filterNot((k, v) -> (v.getTags() == null || v.getTags().containsKey(TagsContents.SQL)))
				// set key as serviceName
				.selectKey((k, v) -> v.getLocalEndpoint() != null
						? v.getLocalEndpoint().getServiceName() + M + v.getName() : v.getName())
				.peek((k, v) -> metricService.initMetricObj(v))
				// 重新映射
				// .mapValues(new ValueMapper<ZipkinLog, TreeObj>() {
				// @Override
				// public TreeObj apply(ZipkinLog v) {
				// TreeObj t = new TreeObj();
				// t.setId(v.getId());
				// t.setName(v.getLocalEndpoint() != null
				// ? v.getLocalEndpoint().getServiceName() + "-" + v.getName() :
				// v.getName());
				// t.setParentId(v.getParentId());
				// return t;
				// }
				// })
				// .groupByKey(Serdes.String(), new
				// JsonSerde<>(ZipkinLog.class))
				// 统计
				// .aggregate(() -> new MetricObj(), new Aggregator<String,
				// ZipkinLog, MetricObj>() {
				// @Override
				// public MetricObj apply(String key, ZipkinLog value, MetricObj
				// aggregate) {
				// aggregate = metricService.initMetricObj(value);
				//
				// return aggregate;
				// }
				//
				// }, new JsonSerde<>(MetricObj.class), storesName)
				// table重新转换为stream
				// .toStream()
				.mapValues(v -> JSON.toJSONString(v))
		//
		;

		out.print();
		return stream;
	}

	/**
	 * 使用kafka-stream-0.11.2版
	 * @param kStreamBuilder
	 * @param aggregator
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	// @Bean
	public KStream<String, String> kStreamV011(KStreamBuilder kStreamBuilder, MetricAggregator aggregator) {
		StateStoreSupplier myStore = Stores.create(storesName).withStringKeys().withStringValues().inMemory().build();
		kStreamBuilder.addStateStore(myStore, null);
		KStream<String, String> stream = kStreamBuilder.stream(topic);

		KStream<String, String> out = stream
				// [(k,v),(k,v),(k,v)...] to TreeObj集合(k,v) key is traceId
				.transform(() -> new TreeObjTransformer(), storesName)
				// change key
				.selectKey((k, v) -> v.getApp() + M + v.getName())
				.groupByKey(Serdes.String(), new JsonSerde<>(TreeObj.class))
				.aggregate(() -> null, aggregator, new JsonSerde<>(MetricObj.class), storesName + "-agg")
				// .peek((k, v) -> metricService.initMetricObj(v))
				.toStream().mapValues(v -> JSON.toJSONString(v))
		//
		;

		out.print();
		return stream;
	}

	/**
	 * 使用kafkastream-1.0版本，部分API迁移到新版
	 * @param kStreamBuilder
	 * @param aggregator
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "deprecation" })
	@Bean
	public KStream<String, String> kStreamV1(StreamsBuilder kStreamBuilder, MetricAggregator aggregator) {
		// StateStoreSupplier myStore =
		// Stores.create(storesName).withStringKeys().withStringValues().inMemory().build();
		StoreBuilder builder = Stores.keyValueStoreBuilder(Stores.inMemoryKeyValueStore(storesName), Serdes.String(),
				Serdes.Bytes()).withCachingEnabled();
		kStreamBuilder.addStateStore(builder);
		KStream<String, String> stream = kStreamBuilder.stream(topic);

		KStream<String, String> out = stream
				// [(k,v),(k,v),(k,v)...] to TreeObj集合(k,v) key is traceId
				.transform(() -> new TreeObjTransformer(), storesName)
				// change key
				.selectKey((k, v) -> v.getApp() + M + v.getName())
				.groupByKey(Serialized.with(Serdes.String(), new JsonSerde<>(TreeObj.class)))
				.aggregate(() -> null, aggregator, new JsonSerde<>(MetricObj.class), storesName + "-agg")
				// .peek((k, v) -> metricService.initMetricObj(v))
				.toStream().mapValues(v -> JSON.toJSONString(v))
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