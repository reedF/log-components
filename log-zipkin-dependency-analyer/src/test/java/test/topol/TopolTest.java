package test.topol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.reed.log.zipkin.dependency.link.TopolLink;
import com.reed.log.zipkin.dependency.link.TopolLinker;
import com.reed.log.zipkin.dependency.utils.SpanComparator;
import com.reed.log.zipkin.dependency.utils.TagsContents;

import scala.Tuple2;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;

/**
 * 测试构建依赖关系
 * @author reed
 *
 */
public class TopolTest {
	private static SpanBytesDecoder spanBytesDecoder = SpanBytesDecoder.JSON_V2;
	private static final String M = "<<>>";

	public static void main(String[] args) {
		testLinks();
	}

	public static String inputStream2String(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + M);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return sb.toString();
	}

	public static List<TopolLink> testLinks() {
		List<TopolLink> sametraceLinks = new ArrayList<>();
		String file = System.getProperty("user.dir") + "\\target\\test-classes\\zipkin.log";
		InputStream input = null;
		try {
			input = new FileInputStream(new File(file));
			List<Span> data = new ArrayList<>();
			Map<String, Set<Span>> sameTraceId = null;
			try {
				String[] jsons = inputStream2String(input).split(M);
				List<String> strs = new ArrayList<>();
				for (String str : jsons) {
					strs.addAll(JSON.parseArray(str, String.class));
				}

				if (strs != null && !strs.isEmpty()) {
					for (String s : strs) {
						Tuple2<String, String> tuple2 = new Tuple2<String, String>(null, s);
						try {
							spanBytesDecoder.decode(tuple2._2.getBytes(Charset.forName("UTF-8")), data);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (data != null && !data.isEmpty()) {
				sameTraceId = new HashMap<>();
				for (Span msg : data) {
					if (msg != null) {
						// make http method name
						if (msg.tags() != null && msg.tags().containsKey(TagsContents.HTTP_PATH)) {
							String path = msg.tags().get(TagsContents.HTTP_PATH);
							if (StringUtils.isNotBlank(path)) {
								msg = msg.toBuilder().name(path).build();
							}
						}
						Set<Span> spans = sameTraceId.get(msg.traceId()) == null ? new TreeSet<>(new SpanComparator())
								: sameTraceId.get(msg.traceId());
						spans.add(msg);
						sameTraceId.put(msg.traceId(), spans);
					}
				}
			}

			if (sameTraceId != null && !sameTraceId.isEmpty()) {
				for (Map.Entry<String, Set<Span>> entry : sameTraceId.entrySet()) {
					if (entry != null && entry.getValue() != null) {
						TopolLinker linker = new TopolLinker();
						linker.putTrace(entry.getValue().iterator());
						List<TopolLink> links = linker.link();
						if (links != null) {
							links.forEach(v -> {
								if (v != null) {
									System.out.println(v.toString());
								}
							});
						}
						sametraceLinks.addAll(linker.getAllLinks());

					}
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		input = null;
		sametraceLinks.forEach(l -> System.out.println("-------" + l.toString()));
		return sametraceLinks;
	}

}
