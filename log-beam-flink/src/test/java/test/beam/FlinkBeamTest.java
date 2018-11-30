package test.beam;

import org.apache.beam.repackaged.beam_sdks_java_io_kafka.com.google.common.collect.ImmutableMap;
import org.apache.beam.runners.flink.FlinkRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.io.kafka.KafkaRecord;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.joda.time.Duration;

public class FlinkBeamTest {
	public static void main(String[] args) {
		// 创建管道工厂
		PipelineOptions options = PipelineOptionsFactory.create();
		// 显式指定PipelineRunner：FlinkRunner必须指定如果不制定则为本地
		options.setRunner(FlinkRunner.class);
		// 设置相关管道
		Pipeline pipeline = Pipeline.create(options);
		// 这里kV后说明kafka中的key和value均为String类型
		PCollection<KafkaRecord<String, String>> lines = pipeline.apply(KafkaIO
				.<String,
						// 必需设置kafka的服务器地址和端口
						String>read()
				.withBootstrapServers(KafkaUtils.borkers).withTopic(KafkaUtils.topic_result)// 必需设置要读取的kafka的topic名称
				.withKeyDeserializer(StringDeserializer.class)// 必需序列化key
				.withValueDeserializer(StringDeserializer.class)// 必需序列化value
				.updateConsumerProperties(ImmutableMap.<String, Object>of("auto.offset.reset", "earliest")));// 这个属性kafka最常见的.
		// 为输出的消息类型。或者进行处理后返回的消息类型
		PCollection<String> kafkadata = lines.apply("Remove Kafka Metadata",
				ParDo.of(new DoFn<KafkaRecord<String, String>, String>() {
					private static final long serialVersionUID = 1L;

					@ProcessElement
					public void processElement(ProcessContext ctx) {
						System.out.print("输出的分区为----：" + ctx.element().getKV());
						ctx.output(ctx.element().getKV().getValue());
					}
				}));
		PCollection<String> windowedEvents = kafkadata
				.apply(Window.<String>into(FixedWindows.of(Duration.standardSeconds(5))));
		PCollection<KV<String, Long>> wordcount = windowedEvents.apply(Count.<String>perElement()); // 统计每一个kafka消息的Count
		PCollection<String> wordtj = wordcount.apply("ConcatResultKVs", MapElements.via( // 拼接最后的格式化输出（Key为Word，Value为Count）
				new SimpleFunction<KV<String, Long>, String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public String apply(KV<String, Long> input) {
						System.out.print("进行统计：" + input.getKey() + ": " + input.getValue());
						return input.getKey() + ": " + input.getValue();
					}
				}));
		wordtj.apply(KafkaIO.<Void, String>write().withBootstrapServers(KafkaUtils.borkers)
				.withTopic(KafkaUtils.topic_result)// 设置返回kafka的消息主题
				// .withKeySerializer(StringSerializer.class)//这里不用设置了，因为上面 Void
				.withValueSerializer(StringSerializer.class)
				// Dataflow runner and Spark 兼容， Flink
				// 对kafka0.11才支持。版本0.10不兼容
				// .withEOS(20, "eos-sink-group-id")
				.values() // 只需要在此写入默认的key就行了，默认为null值
		); // 输出结果
		pipeline.run().waitUntilFinish();
	}
}
