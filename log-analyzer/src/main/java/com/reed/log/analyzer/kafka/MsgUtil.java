package com.reed.log.analyzer.kafka;

import java.util.Map;

import com.alibaba.fastjson.JSON;

/**
 * 解析logstash日志
 * 
 * @author reed
 *
 */
@SuppressWarnings("unchecked")
public class MsgUtil {
	/**
	 * logstash format to map
	 * 
	 * @param msg
	 * @return
	 */
	public static Map<String, Object> msg2Map(Object msg) {
		Map<String, Object> m = null;
		if (msg != null) {
			m = (Map<String, Object>) JSON.parse((String) msg);
		}
		return m;
	}

	/**
	 * get playload form logstash("appname")
	 * 
	 * @param m
	 * @return
	 */
	public static String getAppName(Map<String, Object> m) {
		String r = "";
		if (m != null) {
			r = (String) m.get(MsgConstants.APP);
		}
		return r.isEmpty() ? r : r.toUpperCase();
	}

	/**
	 * get playload form logstash("message")
	 * 
	 * @param m
	 * @return
	 */
	public static Map<String, Object> getData(Map<String, Object> m) {
		if (m != null) {
			return (Map<String, Object>) JSON.parse((String) m.get(MsgConstants.MSG));
		} else {
			return null;
		}
	}

	/**
	 * get some field from data message
	 * 
	 * @param m
	 * @param key
	 * @return
	 */
	public static <T> T getFieldValue(Map<String, Object> m, String key) {
		if (m != null && key != null) {
			return (T) m.get(key);
		} else {
			return null;
		}
	}

	/**
	 * get some field from response data
	 * 
	 * @param m
	 * @param key
	 * @return
	 */
	public static Map<String, Object> getBusinessData(Map<String, Object> m) {
		Map<String, Object> r = null;
		if (m != null) {
			Map<String, Object> data = (Map<String, Object>) m.get(MsgConstants.OUT);
			for (Map.Entry<String, Object> item : data.entrySet()) {
				if (item != null && item.getValue() != null) {
					if (item.getValue() instanceof String) {
						r = msg2Map(item.getValue());
					} else {
						r = (Map<String, Object>) item.getValue();
					}
				}
			}
		}
		return r;
	}
}
