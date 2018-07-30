package test.beam;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

import org.apache.beam.repackaged.beam_sdks_java_io_kafka.com.google.common.collect.ImmutableMap;
import org.apache.beam.runners.direct.DirectRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.transforms.Values;
import org.apache.beam.sdk.transforms.windowing.AfterPane;
import org.apache.beam.sdk.transforms.windowing.AfterProcessingTime;
import org.apache.beam.sdk.transforms.windowing.AfterWatermark;
import org.apache.beam.sdk.transforms.windowing.SlidingWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.joda.time.Instant;

/**
 * Zipkin调用关系统计
 * window使用详情可参见：https://beam.apache.org/documentation/programming-guide/#windowing
 * @author reed
 *
 */
public class KafkaBeamTest {

	public static final String M = "|";

	public static final String borkers = "192.168.59.103:9092";
	public static final String topic = "logs";

	interface WindowingOptions extends PipelineOptions {

		@Description("Path of the IP library file to read from.")
		String getIpFile();

		void setIpFile(String ipFile);

		@Description("Path of the event log file to read from.")
		String getEventFile();

		void setEventFile(String eventFile);

		@Description("Fixed window duration, in seconds.")
		@Default.Integer(5)
		Integer getWindowSizeSecs();

		void setWindowSizeSecs(Integer value);

		@Description("Fixed number of shards to produce per window.")
		@Default.Integer(1)
		Integer getNumShards();

		void setNumShards(Integer numShards);

		@Description("Directory of the output to write to.")
		String getOutputDir();

		void setOutputDir(String outputDir);

		@Description("Prefix of the output file prefix.")
		@Default.String("result")
		String getOutputFilePrefix();

		void setOutputFilePrefix(String outputFilePrefix);
	}

	public static void main(String[] args) {
		args = new String[] { "--ipFile=ips.txt", "--eventFile=test.log", "--outputDir=.", "--outputFilePrefix=result",
				"--windowSizeSecs=3000", "--numShards=1" };
		WindowingOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().as(WindowingOptions.class);
		// options.setRunner(FlinkRunner.class);
		//using DirectRunner for testing and demo
		options.setRunner(DirectRunner.class);
		String path = System.getProperty("user.dir") + "\\target\\test-classes\\";
		String ipFile = path + options.getIpFile();
		String eventFile = path + options.getEventFile();
		String output = new File(path, options.getOutputFilePrefix()).getAbsolutePath();
		Pipeline pipeline = Pipeline.create(options);

		cleanOldResultFile(path, options.getOutputFilePrefix());
		changeLogLevel();
		
		// read input event source
		// PCollection<String> events =
		// kV说明kafka中的key和value均为String类型
		PCollection<String> events = pipeline
				.apply(
						//
						KafkaIO.<String, String>read()
								// 必需，设置kafka的服务器地址和端口
								.withBootstrapServers(borkers).withTopic(topic)// 必需，设置要读取的kafka的topic名称
								.withKeyDeserializer(StringDeserializer.class)// 必需
								.withValueDeserializer(StringDeserializer.class)// 必需
								// 设置后将无界数据流转换为有界数据集合，源数据达到这个量值就会处理,处理完毕后pipeline退出，仅用于测试与demo
								// .withMaxNumRecords(10)
								// 设置PCollection中元素对应的时间戳
								// .withTimestampPolicyFactory()
								// .withProcessingTime()
								// commit offset
								.commitOffsetsInFinalize()
								.updateConsumerProperties(ImmutableMap.<String, Object>of(
										// settings,
										// enable.auto.commit必须配true，否则不消费
										"enable.auto.commit", "true", "auto.offset.reset", "latest",
										ConsumerConfig.GROUP_ID_CONFIG, "test"))
								// meta
								.withoutMetadata())
				// KV to value
				.apply(Values.<String>create())
				// .apply(ParDo.of(new DoFn<KV<String, String>,String>() {
				// @ProcessElement
				// public void processElement(ProcessContext c) {
				// System.out.println(c.element().getKey());
				// c.output(c.element().getValue());
				// }
				// }))
				// Adding timestamps
				.apply(ParDo.of(new DoFn<String, String>() {
					@ProcessElement
					public void processElement(ProcessContext c) {
						// Extract the timestamp from log entry we're
						// currently
						// processing.
						Instant instant = new Instant();
						Instant realTime = instant.plus(Duration.ofHours(8).toMillis());
						// Use ProcessContext.outputWithTimestamp
						// (rather than
						// ProcessContext.output) to emit the entry with
						// timestamp attached.
						c.outputWithTimestamp(c.element(), realTime);
					}
				}));
		// window
		PCollection<String> windowedEvents = events.apply(Window
				.<String>into(
						// 滑动窗口，可使用配置options.getWindowSizeSecs()
						SlidingWindows.of(org.joda.time.Duration.standardSeconds(15))
								.every(org.joda.time.Duration.standardSeconds(5)))
				// trigger定义何时发送窗口内的聚合数据（即pane）
				.triggering(
						// AfterWatermark
						// trigger针对事件时间，定义发送时间为：watermark到达window的结束点
						AfterWatermark.pastEndOfWindow()
								// fire on current data
								.withEarlyFirings(
										// AfterProcessingTime
										// trigger针对处理时间，定义发送时间为：一定处理时间过后发送window聚合数据
										AfterProcessingTime.pastFirstElementInPane()
												.plusDelayOf(org.joda.time.Duration.standardSeconds(1)))
								// AfterPane是个Data-driven
								// trigger，针对事件数量处理，收集到一定数量数据后才发送window
								// Fire on any late data
								.withLateFirings(AfterPane.elementCountAtLeast(3)))
				// 允许多久数据延迟，延迟的数据（数据发生时间在窗口内，但当前处理时间已超出此定义的延迟时间长度）也会算入窗口
				.withAllowedLateness(org.joda.time.Duration.standardSeconds(30))
				// accumulatingFiredPanes与discardingFiredPanes定义windown内聚合结果是否累积
				.accumulatingFiredPanes()
		//
		);

		// count by (window)
		PCollection<KV<String, Long>> counts = windowedEvents.apply(Count.perElement());

		// control to output final result
		// final PTransform<PCollection<String>, PDone> writer = new
		// WriteOneFilePerWindow(output, options.getNumShards());
		//
		// // format & output windowed result
		// counts.apply(MapElements.via(new SimpleFunction<KV<String, Long>,
		// String>() {
		// @Override
		// public String apply(KV<String, Long> input) {
		// return input.getKey() + "\t" + input.getValue() + "\r\n";
		// }
		// })).apply(writer);

		// format & output windowed result
		counts.apply(MapElements.via(new SimpleFunction<KV<String, Long>, String>() {
			@Override
			public String apply(KV<String, Long> input) {
				return input.getKey() + "\t" + input.getValue() + "\r\n";
			}
		}))
				// write to kafka
				.apply(KafkaIO.<Void, String>write().withBootstrapServers(borkers).withTopic("results")
						// just need serializer for value
						.withValueSerializer(StringSerializer.class).values());

		// execute beam pipeline
		PipelineResult result = pipeline.run();
		try {
			result.waitUntilFinish();
		} catch (Exception exception) {
			try {
				result.cancel();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static long parseToTimestamp(String time) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = null;
		try {
			date = sdf.parse(time);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return date.getTime();

	}

	public static void cleanOldResultFile(String path, String prefix) {
		if (StringUtils.isNotBlank(path) && StringUtils.isNotBlank(prefix)) {
			File file = new File(path);
			if (file.isDirectory()) {
				String[] dirs = file.list();
				if (dirs != null) {
					for (String s : dirs) {
						if (StringUtils.isNotBlank(s) && (s.startsWith(".temp-beam") || s.startsWith(prefix))) {
							DeleteFileUtil.delete(path + "/" + s);
						}
					}
				}
			}
		}
	}

	public static void changeLogLevel() {
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getLogger("org.apache.kafka").setLevel(Level.OFF);
	}
}
