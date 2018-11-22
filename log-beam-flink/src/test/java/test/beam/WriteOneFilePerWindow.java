package test.beam;

import org.apache.beam.sdk.io.FileBasedSink;
import org.apache.beam.sdk.io.FileBasedSink.FilenamePolicy;
import org.apache.beam.sdk.io.FileBasedSink.OutputFileHints;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.fs.ResolveOptions.StandardResolveOptions;
import org.apache.beam.sdk.io.fs.ResourceId;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;
import org.apache.beam.sdk.transforms.windowing.IntervalWindow;
import org.apache.beam.sdk.transforms.windowing.PaneInfo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * A {@link DoFn} that writes elements to files with names deterministically derived from the lower
 * and upper bounds of their key (an {@link IntervalWindow}).
 *
 * <p>This is test utility code, not for end-users, so examples can be focused on their primary
 * lessons.
 */
public class WriteOneFilePerWindow extends PTransform<PCollection<String>, PDone> {
	private static final DateTimeFormatter FORMATTER = ISODateTimeFormat.hourMinute();
	private String filenamePrefix;
	private Integer numShards;

	public WriteOneFilePerWindow(String filenamePrefix, Integer numShards) {
		this.filenamePrefix = filenamePrefix;
		this.numShards = numShards;
	}

	@Override
	public PDone expand(PCollection<String> input) {
		ResourceId resource = FileBasedSink.convertToFileResourceIfPossible(filenamePrefix);
		TextIO.Write write = TextIO.write().to(new PerWindowFiles(resource))
				//
				.withTempDirectory(resource.getCurrentDirectory())
				//
				.withWindowedWrites();
		if (numShards != null) {
			write = write.withNumShards(numShards);
		}
		return input.apply(write);
	}

	/**
	 * A {@link FilenamePolicy} produces a base file name for a write based on metadata about the data
	 * being written. This always includes the shard number and the total number of shards. For
	 * windowed writes, it also includes the window and pane index (a sequence number assigned to each
	 * trigger firing).
	 */
	public static class PerWindowFiles extends FilenamePolicy {

		private final ResourceId baseFilename;

		public PerWindowFiles(ResourceId baseFilename) {
			this.baseFilename = baseFilename;
		}

		public String filenamePrefixForWindow(IntervalWindow window) {
			String prefix = baseFilename.isDirectory() ? "" : baseFilename.getFilename();
			return String.format("%s-%s-%s", prefix, FORMATTER.print(window.start()).replaceAll(":", ""),
					FORMATTER.print(window.end()).replaceAll(":", ""));
		}

		@Override
		public ResourceId windowedFilename(int shardNumber, int numShards, BoundedWindow window, PaneInfo paneInfo,
				OutputFileHints outputFileHints) {
			IntervalWindow intervalWindow = (IntervalWindow) window;
			String filename = String.format("%s-%s-of-%s%s", filenamePrefixForWindow(intervalWindow), shardNumber,
					numShards, outputFileHints.getSuggestedFilenameSuffix());
			return baseFilename.getCurrentDirectory().resolve(filename, StandardResolveOptions.RESOLVE_FILE);
		}

		@Override
		public ResourceId unwindowedFilename(int shardNumber, int numShards, OutputFileHints outputFileHints) {
			throw new UnsupportedOperationException("Unsupported.");
		}
	}
}