package com.reed.log.zipkin.dependency.stream;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.reed.log.zipkin.dependency.link.TopolLink;
import com.reed.log.zipkin.dependency.link.TopolLinker;
import com.reed.log.zipkin.dependency.utils.TagsContents;

import scala.Tuple2;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;
import zipkin2.codec.SpanBytesEncoder;

/**
 *
 *
 */
public class TopolLinkTransformer implements Transformer<String, String, KeyValue<String, TopolLink>> {

	public static Logger logger = LoggerFactory.getLogger(TopolLinkTransformer.class);
	private ProcessorContext context;
	private KeyValueStore<String, Bytes> state;
	private SpanBytesDecoder spanBytesDecoder = SpanBytesDecoder.JSON_V2;
	private SpanBytesEncoder spanBytesEncoder = SpanBytesEncoder.JSON_V2;

	@Override
	public void init(ProcessorContext context) {
		this.context = context;
		this.state = (KeyValueStore<String, Bytes>) context.getStateStore(KafkaStreamsConfig.storesName);
		context.schedule(5000); // call #punctuate() each 1000ms
	}

	@Override
	public KeyValue<String, TopolLink> transform(String key, String value) {
		List<Span> data = new ArrayList<>();
		Map<String, Set<Span>> sameTraceId = null;
		try {
			List<String> strs = JSON.parseArray(value, String.class);
			if (strs != null && !strs.isEmpty()) {
				for (String s : strs) {
					Tuple2<String, String> tuple2 = new Tuple2<String, String>(null, s);
					try {
						spanBytesDecoder.decode(tuple2._2.getBytes(Charset.forName("UTF-8")), data);
					} catch (Exception e) {
						logger.warn("Unable to decode span from String:{},ex is :{}", s, e);
					}
				}
			}
		} catch (JSONException e) {
			logger.error("JSON parse error:{},msg is:{}", e.getMessage(), value);
		}

		if (data != null && !data.isEmpty()) {
			sameTraceId = new HashMap<>();
			for (Span msg : data) {
				if (msg != null) {
					// make http method name
					if (msg.tags() != null && msg.tags().containsKey(TagsContents.HTTP_PATH)) {
						String path = msg.tags().get(TagsContents.HTTP_PATH);
						if (StringUtils.isNotBlank(path)) {
							msg.toBuilder().name(path);
						}
					}
					Set<Span> spans = sameTraceId.get(msg.traceId()) == null ? new LinkedHashSet<>()
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
								this.context.forward(entry.getKey(), v);
							}
						});
					}
				}
			}
		}

		return null;

	}

	/**
	 * 定时
	 */
	@Override
	public KeyValue<String, TopolLink> punctuate(long timestamp) {

		return null;
	}

	@Override
	public void close() {
		this.state.close();
	}


}
