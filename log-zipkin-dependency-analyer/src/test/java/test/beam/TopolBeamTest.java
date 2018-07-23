package test.beam;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.TextIO;
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
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.transforms.DoFn.ProcessElement;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.SlidingWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.beam.sdk.values.PDone;
import org.apache.commons.lang.StringUtils;
import org.joda.time.Instant;

import com.reed.log.zipkin.dependency.link.TopolLink;
import com.reed.log.zipkin.dependency.link.TopolLinkBytesDecoder;
import com.reed.log.zipkin.dependency.link.TopolLinkBytesEncoder;

import test.topol.TopolTest;

/**
 * 访问日志统计IP
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
				"--windowSizeSecs=300", "--numShards=2" };
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
		PCollection<String> windowedEvents = events.apply(
				// Fixed-time windows
				// Window.into(FixedWindows.of(org.joda.time.Duration.standardSeconds(options.getWindowSizeSecs())))
				// Sliding time windows
				Window.into(SlidingWindows.of(org.joda.time.Duration.standardSeconds(options.getWindowSizeSecs()))
						.every(org.joda.time.Duration.standardSeconds(60)))
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
