package test.beam;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.transforms.windowing.AfterPane;
import org.apache.beam.sdk.transforms.windowing.AfterProcessingTime;
import org.apache.beam.sdk.transforms.windowing.AfterWatermark;
import org.apache.beam.sdk.transforms.windowing.SlidingWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.apache.commons.lang.StringUtils;
import org.joda.time.Instant;

import com.reed.log.zipkin.dependency.link.TopolLink;

import test.topol.TopolTest;

/**
 * Zipkin调用关系统计
 * window使用详情可参见：https://beam.apache.org/documentation/programming-guide/#windowing
 * @author reed
 *
 */
public class TopolBeamTest {

	public static final String M = "|";

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
				"--windowSizeSecs=3000", "--numShards=2" };
		WindowingOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().as(WindowingOptions.class);
		String path = System.getProperty("user.dir") + "\\target\\test-classes\\";
		String ipFile = path + options.getIpFile();
		String eventFile = path + options.getEventFile();
		String output = new File(path, options.getOutputFilePrefix()).getAbsolutePath();
		Pipeline pipeline = Pipeline.create(options);

		cleanOldResultFile(path, options.getOutputFilePrefix());

		List<TopolLink> links = TopolTest.testLinks();
		List<KV<String, Long>> kvs = new ArrayList<>();
		if (links != null) {
			for (TopolLink t : links) {
				if (t != null) {
					kvs.add(KV.of(t.parent() + M + t.child(), t.timestamp()));
				}
			}
		}

		// read input event source
		PCollection<String> events = pipeline.apply(Create.of(kvs))
				// Adding timestamps
				.apply(ParDo.of(new DoFn<KV<String, Long>, String>() {
					@ProcessElement
					public void processElement(ProcessContext c) {
						long t = c.element().getValue();
						// Extract the timestamp from log entry we're currently
						// processing.
						Instant instant = new Instant(t);
						Instant realTime = instant.plus(Duration.ofHours(8).toMillis());
						// Use ProcessContext.outputWithTimestamp (rather than
						// ProcessContext.output) to emit the entry with
						// timestamp attached.
						c.outputWithTimestamp(c.element().getKey(), realTime);
					}
				}));

		// configure windowing settings
		// PCollection<String> windowedEvents = events.apply(
		// // Fixed-time windows
		// //
		// Window.into(FixedWindows.of(org.joda.time.Duration.standardSeconds(options.getWindowSizeSecs())))
		// // Sliding time windows
		// Window.into(SlidingWindows.of(org.joda.time.Duration.standardSeconds(options.getWindowSizeSecs()))
		// .every(org.joda.time.Duration.standardSeconds(60)))
		// //
		// );

		PCollection<String> windowedEvents = events.apply(Window
				.<String>into(
						// 滑动窗口，可使用配置options.getWindowSizeSecs()
						SlidingWindows.of(org.joda.time.Duration.standardMinutes(15))
								.every(org.joda.time.Duration.standardMinutes(5)))
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
												.plusDelayOf(org.joda.time.Duration.standardMinutes(1)))
								// AfterPane是个Data-driven
								// trigger，针对事件数量处理，收集到一定数量数据后才发送window
								// Fire on any late data
								.withLateFirings(AfterPane.elementCountAtLeast(3)))
				// 允许多久数据延迟，延迟的数据（数据发生时间在窗口内，但当前处理时间已超出此定义的延迟时间长度）也会算入窗口
				.withAllowedLateness(org.joda.time.Duration.standardMinutes(30))
				//accumulatingFiredPanes与discardingFiredPanes定义windown内聚合结果是否累积
				.accumulatingFiredPanes()
		//
		);

		// count by (window)
		PCollection<KV<String, Long>> counts = windowedEvents.apply(Count.perElement());

		// control to output final result
		final PTransform<PCollection<String>, PDone> writer = new WriteOneFilePerWindow(output, options.getNumShards());

		// format & output windowed result
		counts.apply(MapElements.via(new SimpleFunction<KV<String, Long>, String>() {
			@Override
			public String apply(KV<String, Long> input) {
				return input.getKey() + "\t" + input.getValue() + "\r\n";
			}
		})).apply(writer);

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

}
