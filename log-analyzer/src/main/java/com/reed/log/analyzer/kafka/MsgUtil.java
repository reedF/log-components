package com.reed.log.analyzer.kafka;

import java.util.Map;

import com.alibaba.fastjson.JSON;
/**
 * 解析logstash日志
 * @author reed
 *
 */
@SuppressWarnings("unchecked")
public class MsgUtil {

	public static Map<String, Object> msg2Map(Object msg) {
		Map<String, Object> m = null;
		if (msg != null) {
			m = (Map<String, Object>) JSON.parse((String) msg);
		}
		return m;
	}

	public static Map<String, Object> getData(Map<String, Object> m) {
		if (m != null) {
			return (Map<String, Object>) JSON.parse((String) m.get("message"));
		} else {
			return null;
		}
	}

	public static <T> T getFieldValue(Map<String, Object> m, String key) {
		if (m != null && key != null) {
			return (T) m.get(key);
		} else {
			return null;
		}
	}
}
