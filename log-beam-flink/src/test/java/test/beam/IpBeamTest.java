package test.beam;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.transforms.windowing.SlidingWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.beam.sdk.values.PDone;
import org.apache.commons.lang.StringUtils;
import org.joda.time.Instant;

import com.reed.log.common.JobConfig;
import com.reed.log.common.RunnerTypeEnum;

/**
 * 访问日志统计IP
 * @author reed
 *
 */
public class IpBeamTest {

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
				"--windowSizeSecs=60", "--numShards=2" };
		WindowingOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().as(WindowingOptions.class);
		options.setRunner(RunnerTypeEnum.getRunner(JobConfig.RUNNER_TYPE_DIRECT));
		String path = System.getProperty("user.dir") + "\\target\\test-classes\\";
		String ipFile = path + options.getIpFile();
		String eventFile = path + options.getEventFile();
		String output = new File(path, options.getOutputFilePrefix()).getAbsolutePath();
		Pipeline pipeline = Pipeline.create(options);

		cleanOldResultFile(path, options.getOutputFilePrefix());

		// ipview
		final PCollectionView<Map<String, String>> ipToAreaMapView = pipeline.apply(TextIO.read().from(ipFile))
				.apply(ParDo.of(new DoFn<String, KV<String, String>>() {
					@ProcessElement
					public void processElement(ProcessContext c) {
						String[] ipAreaPair = c.element().split("-");
						if (ipAreaPair.length == 2) {
							c.output(KV.of(ipAreaPair[0], ipAreaPair[1]));
						}
					}
				})).apply(View.<String, String>asMap());

		// read input event source
		PCollection<String> events = pipeline.apply(TextIO.read().from(eventFile))
				.apply(ParDo.of(new DoFn<String, String>() {
					@ProcessElement
					public void processElement(ProcessContext c) {
						String event = c.element();
						try {
							String[] a = event.split("\\s+");
							String ip = a[0];
							String time = a[3] + " " + a[4];
							time = time.replace("[", "").replace("]", "");

							long ts = parseToTimestamp(time);
							Instant instant = new Instant(ts);
							Instant realTime = instant.plus(Duration.ofHours(8).toMillis());

							String area = c.sideInput(ipToAreaMapView).get(ip);
							area = (area == null ? "未知地域" : area);
							c.outputWithTimestamp(area, realTime);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				})
						// 旁路输入
						.withSideInputs(ipToAreaMapView));

		// configure windowing settings
		PCollection<String> windowedEvents = events.apply(
				// Fixed-time windows
				// Window.<String>into(FixedWindows.of(org.joda.time.Duration.standardSeconds(options.getWindowSizeSecs())))
				// Sliding time windows
				Window.<String>into(
						SlidingWindows.of(org.joda.time.Duration.standardSeconds(options.getWindowSizeSecs()))
								.every(org.joda.time.Duration.standardSeconds(5)))
		//
		);

		// count by (window, area)
		PCollection<KV<String, Long>> areaCounts = windowedEvents.apply(Count.<String>perElement());

		// control to output final result
		// final PTransform<PCollection<String>, PDone> writer = new
		// PerWindowOneFileWriter(output,
		// options.getNumShards());
		final PTransform<PCollection<String>, PDone> writer = new WriteOneFilePerWindow(output, options.getNumShards());

		// format & output windowed result
		areaCounts.apply(MapElements.via(new SimpleFunction<KV<String, Long>, String>() {
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
		} finally {
			System.out.println("Done!");
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
