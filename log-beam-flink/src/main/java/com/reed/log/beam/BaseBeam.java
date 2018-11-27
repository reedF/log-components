package com.reed.log.beam;

import java.io.IOException;
import java.time.Duration;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.ParDo.SingleOutput;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.joda.time.Instant;

import com.alibaba.fastjson.JSON;
import com.reed.log.common.JobConfig;
import com.reed.log.common.JobOptions;
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
		} catch (Exception exception) {
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
		PCollection<T> events = pipeline.apply(
				//
				KafkaIO.<String, String>read()
						// 必需，设置kafka的服务器地址和端口
						.withBootstrapServers(JobConfig.getKafkaBrokers()).withTopics(JobConfig.getKafkaTopicsInput())// 必需，设置要读取的kafka的topic名称
						.withKeyDeserializer(StringDeserializer.class)// 必需
						.withValueDeserializer(StringDeserializer.class)// 必需
						// 设置后将无界数据流转换为有界数据集合，源数据达到这个量值就会处理,处理完毕后pipeline退出，仅用于测试与demo
						// .withMaxNumRecords(10)
						// 设置PCollection中元素对应的时间戳
						// .withTimestampPolicyFactory()
						// .withProcessingTime()
						// commit offset
						.commitOffsetsInFinalize().updateConsumerProperties(KafkaConfig.getConsumerProperties())
						// meta
						.withoutMetadata())
				.apply(addProcessingTs())
				// KV to value
				// .apply(Values.<T>create())
				.apply(ParDo.of(new DoFn<KV<String, String>, T>() {
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

		return events;
	}

	public static void writeToKafka(PCollection<String> events, String borkers, String topic) {
		if (events != null) {
			events.apply(KafkaIO.<String, String>write().withBootstrapServers(borkers).withTopic(topic)
					.withValueSerializer(StringSerializer.class).values());
		}
	}

	@SuppressWarnings("serial")
	public static <K, V> PCollection<String> formatResult(PCollection<KV<K, V>> events) {
		return events.apply(MapElements.via(new SimpleFunction<KV<K, V>, String>() {
			@Override
			public String apply(KV<K, V> input) {
				String key = JSON.toJSONString(input.getKey());
				String data = JSON.toJSONString(input.getValue());
				return key + "===" + data;
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
