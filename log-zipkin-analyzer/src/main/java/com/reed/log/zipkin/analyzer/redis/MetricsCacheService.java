package com.reed.log.zipkin.analyzer.redis;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.reed.log.zipkin.analyzer.tree.TreeObj;

@Service
public class MetricsCacheService {

	private final static String POINT = ".";
	private final static String CACHE = "metrics.appname";
	private final static String MAX = "metrics.max.";

	@Autowired
	private StringRedisTemplate template;

	@Autowired
	private RedisTemplate<String, Double> maxTemplate;

	/**
	 * 缓存统计项
	 * @param app
	 * @param value
	 */
	public void cache(String app, TreeObj value) {
		if (app != null) {
			template.boundSetOps(CACHE).add(app);
			String key = genKey(app);
			template.boundSetOps(key).add(JSON.toJSONString(value));
			template.expireAt(key, getExpireDate());
		}
	}

	/**
	 * 查询全部应用
	 * 
	 * @return
	 */
	public Set<String> getAppNames() {
		Set<String> r = new HashSet<>();
		if (template.hasKey(CACHE)) {
			r = template.opsForSet().members(CACHE);
		}
		return r;
	}

	/**
	 * 查询从该应用发起的全部根节点trace(root span)
	 * @param app
	 * @return
	 */
	public Set<String> getMetricNames(String app) {
		Set<String> r = new HashSet<>();
		String key = genKey(app);
		if (template.hasKey(key)) {
			Set<String> sets = template.opsForSet().members(key);
			if (sets != null) {
				for (String s : sets) {
					if (s != null) {
						TreeObj t = JSON.parseObject(s, TreeObj.class);
						if (t != null) {
							r.add(t.getName());
						}
					}
				}
			}
		}
		return r;
	}

	/**
	 * 查询从该应用发起的全部跟踪树（完整跟踪链）
	 * @param app
	 * @return
	 */
	public Set<TreeObj> getAllMetricItem(String app) {
		Set<TreeObj> r = new HashSet<>();
		String key = genKey(app);
		if (template.hasKey(key)) {
			Set<String> sets = template.opsForSet().members(key);
			if (sets != null) {
				for (String s : sets) {
					if (s != null) {
						TreeObj t = JSON.parseObject(s, TreeObj.class);
						if (t != null) {
							r.add(t);
						}
					}
				}
			}
		}
		return r;
	}

	/**
	 * 根据名称获取跟踪树
	 * @param app
	 * @param name
	 * @return
	 */
	public Set<TreeObj> getMetricItem(String app, String name) {
		Set<TreeObj> r = new HashSet<>();
		String key = genKey(app);
		if (template.hasKey(key)) {
			Set<String> sets = template.opsForSet().members(key);
			if (sets != null) {
				for (String s : sets) {
					if (s != null) {
						TreeObj t = JSON.parseObject(s, TreeObj.class);
						if (t != null && name.equals(t.getName())) {
							r.add(t);
						}
					}
				}
			}
		}
		return r;
	}

	public Double addMaxValue(String key, Double value) {
		key = MAX + key;
		if (maxTemplate.hasKey(key)) {
			Double old = maxTemplate.opsForValue().get(key);
			if (value > old) {
				maxTemplate.opsForValue().set(key, value);
			} else {
				value = old;
			}
		} else {
			maxTemplate.opsForValue().set(key, value);
		}
		// set expire time to 00:00:00
		maxTemplate.expireAt(key, getExpireDate());
		return value;
	}

	public Double getMaxValue(String key) {
		return maxTemplate.opsForValue().get(key);
	}

	private String genKey(String app) {
		return CACHE + POINT + app;
	}

	/**
	 * 获取下一天零点
	 * @return
	 */
	private Date getExpireDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		return calendar.getTime();
	}
}
