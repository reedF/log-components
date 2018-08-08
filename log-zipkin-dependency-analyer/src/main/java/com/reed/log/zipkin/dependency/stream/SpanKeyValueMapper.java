package com.reed.log.zipkin.dependency.stream;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;

import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;
import zipkin2.codec.SpanBytesEncoder;

/**
 * zipkin log msg:[(k,v),(k,v),(k,v)...] to 集合(k,v) key is traceId
 * @author reed
 *
 */
public class SpanKeyValueMapper implements KeyValueMapper<String, String, Iterable<KeyValue<String, String>>> {

	public static Logger logger = LoggerFactory.getLogger(SpanKeyValueMapper.class);

	public Iterable<KeyValue<String, String>> apply(String key, String v) {
		Set<Span> data = new HashSet<>();
		Map<String, List<Span>> sameTraceId = new HashMap<>();
		List<KeyValue<String, String>> kvs = new ArrayList<>();
		try {
			List<String> strs = JSON.parseArray(v, String.class);
			if (strs != null && !strs.isEmpty()) {
				for (String s : strs) {
					try {
						SpanBytesDecoder.JSON_V2.decode(s.getBytes(Charset.forName("UTF-8")), data);
					} catch (Exception e) {
						logger.warn("Unable to decode span from String:{},ex is :{}", s, e);
					}
				}
			}
		} catch (JSONException e) {
			logger.error("JSON parse error:{},msg is:{}", e.getMessage(), v);
		}

		if (data != null && !data.isEmpty()) {
			for (Span msg : data) {
				if (msg != null && StringUtils.isNotBlank(msg.traceId())) {
					List<Span> spans = sameTraceId.get(msg.traceId()) == null ? new ArrayList<>()
							: sameTraceId.get(msg.traceId());
					spans.add(msg);
					sameTraceId.put(msg.traceId(), spans);
				}
			}
			if (sameTraceId != null && !sameTraceId.isEmpty()) {
				for (Map.Entry<String, List<Span>> entry : sameTraceId.entrySet()) {
					if (entry != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
						String str = new String(SpanBytesEncoder.JSON_V2.encodeList(entry.getValue()),
								Charset.forName("UTF-8"));
						kvs.add(new KeyValue<String, String>(entry.getKey(), str));
					}
				}
			}
		}
		return kvs;
	}
}
