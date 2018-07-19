package com.reed.log.zipkin.dependency.stream;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.apache.commons.lang.StringUtils;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.reed.log.zipkin.dependency.link.TopolLink;
import com.reed.log.zipkin.dependency.link.TopolLinker;
import com.reed.log.zipkin.dependency.utils.SpanComparator;
import com.reed.log.zipkin.dependency.utils.TagsContents;

import scala.Tuple2;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;
import zipkin2.codec.SpanBytesEncoder;

/**
 *转换json为span，并计算TopolLink
 *
 */
public class TopolLinkTransformer implements Transformer<String, String, KeyValue<String, TopolLink>> {

	public static Logger logger = LoggerFactory.getLogger(TopolLinkTransformer.class);
	private ProcessorContext context;
	private KeyValueStore<String, Bytes> state;
	private SpanBytesDecoder spanBytesDecoder = SpanBytesDecoder.JSON_V2;
	private SpanBytesEncoder spanBytesEncoder = SpanBytesEncoder.JSON_V2;
	// 缓存数据过期水位线，15分钟
	public static final long waterMark = 15;

	@Override
	public void init(ProcessorContext context) {
		this.context = context;
		this.state = (KeyValueStore<String, Bytes>) context.getStateStore(KafkaStreamsConfig.storesName);
		context.schedule(60000); // call #punctuate() each 1000ms
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
			// sameTraceId = new HashMap<>();
			sameTraceId = getAllFromStore();
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
			saveInStore(sameTraceId);
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
	 * 定时,刷新缓存，保持缓存内的span都是数据水位线（waterMark）有效期内的
	 */
	@Override
	public KeyValue<String, TopolLink> punctuate(long timestamp) {
		flushStore();
		return null;
	}

	@Override
	public void close() {
		this.state.close();
	}

	/**
	 * 缓存同一个trace内的span集合
	 * @param sameTraceId
	 */
	private void saveInStore(Map<String, Set<Span>> sameTraceId) {
		if (sameTraceId != null) {
			for (Map.Entry<String, Set<Span>> entry : sameTraceId.entrySet()) {
				if (entry != null) {
					this.state.put(entry.getKey(),
							Bytes.wrap(spanBytesEncoder.encodeList(new ArrayList<>(entry.getValue()))));
				}
			}
		}
	}

	/**
	 * 从store内获取缓存的合并同一个trace内的span集合
	 * @return
	 */
	private Map<String, Set<Span>> getAllFromStore() {
		Map<String, Set<Span>> map = new HashMap<>();
		KeyValueIterator<String, Bytes> iterator = this.state.all();
		if (iterator != null) {
			iterator.forEachRemaining(entry -> {
				if (entry != null) {
					Set<Span> sets = new HashSet<>();
					spanBytesDecoder.decodeList(entry.value.get(), sets);
					map.put(entry.key, sets);
				}
			});
		}
		return map;
	}

	private void flushStore() {
		if (this.state != null) {
			Predicate<Span> predicate = (s) -> getDistanceTime(System.currentTimeMillis(), s.timestampAsLong(),
					waterMark);
			Map<String, Set<Span>> sameTraceId = getAllFromStore();
			for (Map.Entry<String, Set<Span>> entry : sameTraceId.entrySet()) {
				if (entry != null && entry.getValue() != null) {
					Set<Span> set = entry.getValue();
					set.removeIf(predicate);
					sameTraceId.put(entry.getKey(), set);
				}
			}
			saveInStore(sameTraceId);
		}
	}

	/**
	 * 是否相差waterMark分钟
	 * @param time1
	 * @param time2
	 * @return
	 */
	public static boolean getDistanceTime(long time1, long time2, long waterMark) {
		boolean r = false;
		long diff = time1 - time2;
		r = diff / (60 * 1000) - waterMark > 0;

		return r;
	}

}
