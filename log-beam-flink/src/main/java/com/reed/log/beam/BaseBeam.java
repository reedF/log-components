package com.reed.log.beam;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;

import org.apache.beam.repackaged.beam_sdks_java_io_kafka.com.google.common.collect.ImmutableMap;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.elasticsearch.ElasticsearchIO;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.ParDo.SingleOutput;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.joda.time.Instant;

import com.alibaba.fastjson.JSON;
import com.reed.log.common.JobConfig;
import com.reed.log.common.JobOptions;
import com.reed.log.es.EsConfig;
import com.reed.log.kafka.KafkaConfig;
import com.reed.log.model.BaseObj;

import lombok.extern.slf4j.Slf4j;

/**
 * beam基础类，定义公共方法
 * @author reed
 *
 */
@Slf4j
public abstract class BaseBeam {

	/**
	 * 业务逻辑
	 * @param pipeline
	 */
	public abstract void doBusiness(Pipeline pipeline);

	public static void runningJob(Pipeline pipeline, BaseBeam beam) {
		pipeline.getOptions().setJobName(beam.getClass().getName());
		beam.doBusiness(pipeline);
		executePipeline(pipeline);
	}

	public static Pipeline initPipeline(String[] args) {
		JobOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().as(JobOptions.class);
		options.setRunner(JobConfig.getRunner());
		return Pipeline.create(options);
	}

	public static void executePipeline(Pipeline pipeline) {
		// execute beam pipeline
		PipelineResult result = pipeline.run();
		try {
			result.waitUntilFinish();
		} catch (Exception ex) {
			log.error("======Job running error:{}======", ex.getMessage());
			try {
				result.cancel();
			} catch (IOException e) {
				log.error("======Job running error:{}======", e.getMessage());
			}
		} finally {
			log.info("======Job stop======");
		}
	}

	/**
	 * adding Extract the timestamp from log entry we're currently processing.
	 * @return
	 */
	@SuppressWarnings("serial")
	public static <InputT> SingleOutput<InputT, InputT> addProcessingTs() {
		return ParDo.of(new DoFn<InputT, InputT>() {
			@ProcessElement
			public void processElement(ProcessContext c) {
				Instant instant = new Instant();
				Instant realTime = instant.plus(Duration.ofHours(8).toMillis());
				// Use ProcessContext.outputWithTimestamp
				// (rather than
				// ProcessContext.output) to emit the entry with
				// timestamp attached.
				c.outputWithTimestamp(c.element(), realTime);
			}
		});
	}

	/**
	 * read msg from kafka and transform to T
	 * @param pipeline
	 * @return
	 */
	@SuppressWarnings("serial")
	public static <T extends BaseObj> PCollection<T> readFromKafka(Pipeline pipeline, Class<T> type) {
		PCollection<T> events = pipeline.apply("kafka-source",
				//
				KafkaIO.<String, String>read()
						// 必需，设置kafka的服务器地址和端口
						.withBootstrapServers(JobConfig.getKafkaBrokers()).withTopics(JobConfig.getKafkaTopicsInput())// 必需，设置要读取的kafka的topic名称
						.withKeyDeserializer(StringDeserializer.class)// 必需
						.withValueDeserializer(StringDeserializer.class)// 必需
						.updateConsumerProperties(KafkaConfig.getConsumerProperties())
						// 设置后将无界数据流转换为有界数据集合，源数据达到这个量值就会处理,处理完毕后pipeline退出，仅用于测试与demo
						// .withMaxNumRecords(10)
						// 设置PCollection中元素对应的时间戳
						// .withTimestampPolicyFactory()
						// .withProcessingTime()
						// commit offset
						.commitOffsetsInFinalize()
						// meta
						.withoutMetadata())
				.apply("adding-ts", addProcessingTs())
				// KV to value
				// .apply(Values.<T>create())
				.apply("transform-kafka-msg", ParDo.of(new DoFn<KV<String, String>, T>() {
					@ProcessElement
					public void processElement(ProcessContext c) {
						// Type type = new TypeReference<T>() {}.getType();
						if (c.element().getValue() != null) {
							T t = JSON.parseObject(c.element().getValue(), type);
							if (t instanceof BaseObj) {
								t.setMsgKey(c.element().getKey());
							}
							c.output(t);
						}
					}
				}));
		log.info("======read from kafka source done!=====");
		return events;
	}

	/**
	 * write to kafka
	 * refer to https://beam.apache.org/releases/javadoc/2.0.0/org/apache/beam/sdk/io/kafka/KafkaIO.html
	 * @param events
	 * @param borkers
	 * @param topic
	 */
	public static void writeToKafka(PCollection<KV<String, String>> events, String borkers, String topic) {
		if (events != null) {
			events.apply("kafka-sink",
					KafkaIO.<String, String>write()
							// setting
							.withBootstrapServers(borkers).withTopic(topic)
							// ser
							.withKeySerializer(StringSerializer.class).withValueSerializer(StringSerializer.class)
							// settings for ProducerConfig. e.g, to enable
							// compression :
							.updateProducerProperties(ImmutableMap.of("compression.type", "gzip")));
		}
		log.info("======write to kafka sink done!=====");
	}

	@SuppressWarnings("serial")
	public static void writeToEs(PCollection<KV<String, String>> events, String[] esCluster, String indexName,
			String indexType) {
		if (events != null) {
			String indexDate = DateFormatUtils.format(new Date(), "yyyyMMdd");
			events
					// kv to v
					.apply("kv-to-v", ParDo.of(new DoFn<KV<String, String>, String>() {
						@ProcessElement
						public void processElement(ProcessContext c) {
							c.output(c.element().getValue());
						}
					}))
					// write to es
					.apply("es-sink",
							ElasticsearchIO.write()
									// config
									.withConnectionConfiguration(EsConfig.initEsConfig(esCluster, indexDate, indexType))
			// retry
			// .withRetryConfiguration(ElasticsearchIO.RetryConfiguration.create(10,
			// org.joda.time.Duration.standardMinutes(3)))
			//
			);
		}
		log.info("======write to es sink done!=====");
	}

	@SuppressWarnings("serial")
	public static <K, V> PCollection<KV<String, String>> formatResult(PCollection<KV<K, V>> events) {
		return events.apply(MapElements.via(new SimpleFunction<KV<K, V>, KV<String, String>>() {
			@Override
			public KV<String, String> apply(KV<K, V> input) {
				String key = JSON.toJSONString(input.getKey());
				String value = JSON.toJSONString(input.getValue());
				return KV.<String, String>of(key, value);
			}
		}));
	}

	/**
	 * just for testing
	 * @param events
	 */
	@SuppressWarnings("serial")
	public static <T extends BaseObj> void logMsg(PCollection<T> events) {
		events.apply(ParDo.of(new DoFn<T, T>() {
			@ProcessElement
			public void processElement(ProcessContext c) {
				log.info("=======get msg:{}========", c.element().toString());
				c.output(c.element());
			}
		}));
	}
}
