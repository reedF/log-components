package com.reed.log.zipkin.analyzer.stream;

import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.reed.log.zipkin.analyzer.pojo.ZipkinLog;

/**
 * zipkin log msg:[(k,v),(k,v),(k,v)...] to 集合(k,v) key is traceId
 * @author reed
 *
 */
public class ZipkinKeyValueMapper implements KeyValueMapper<String, String, Iterable<KeyValue<String, ZipkinLog>>> {
	
	public static Logger logger = LoggerFactory.getLogger(ZipkinKeyValueMapper.class);

	public Iterable<KeyValue<String, ZipkinLog>> apply(String key, String v) {
		List<ZipkinLog> data = null;
		List<KeyValue<String, ZipkinLog>> kvs = new ArrayList<>();
		try {
			data = JSON.parseArray(v, ZipkinLog.class);
		} catch (JSONException e) {
			logger.error("JSON parse error:{},msg is:{}", e.getMessage(), v);
		}

		if (data != null && !data.isEmpty()) {
			for (ZipkinLog msg : data) {
				if (msg != null) {
					kvs.add(new KeyValue<String, ZipkinLog>(msg.getTraceId(), msg));
				}
			}
		}
		return kvs;
	}
}
