package com.reed.log.zipkin.analyzer.alarm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.alibaba.fastjson.JSON;

/**
 * 报警服务抽象默认实现
 * @author reed
 *
 * @param <T>
 */
public abstract class AlarmBaseService<T> implements AlarmService<T> {

	public static final String CACHE_PRX = "alarm.";

	public static final String CACHE_ITEM_KEY = "alarm.items";

	@Autowired
	private StringRedisTemplate cache;

	@Autowired
	private AlarmItemRepository alarmItemRepository;

	public List<AlarmItem> findAllAlarmItemByEs(Boolean enable) {
		List<AlarmItem> r = null;
		QueryBuilder queryBuilder;
		if (enable != null) {
			queryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("enable", enable));
		} else {
			queryBuilder = QueryBuilders.matchAllQuery();
		}
		Iterable<AlarmItem> iter = alarmItemRepository.search(queryBuilder);
		if (iter != null) {
			r = new ArrayList<>();
			Iterator<AlarmItem> itor = iter.iterator();
			while (itor.hasNext()) {
				AlarmItem a = itor.next();
				r.add(a);
				cacheItem(a);
			}
		}

		return r;
	}

	@Override
	public AlarmItem findAlarmItemById(Long id) {
		AlarmItem r = null;
		if (id != null) {
			r = alarmItemRepository.findById(id).orElse(r);
		}
		return r;
	}

	@Override
	public List<AlarmItem> findAllAlarmItem(Boolean enable) {
		List<AlarmItem> r = null;
		if (cache.hasKey(CACHE_ITEM_KEY)) {
			Set<String> sets = cache.opsForSet().members(CACHE_ITEM_KEY);
			if (sets != null && !sets.isEmpty()) {
				r = new ArrayList<>();
				for (String s : sets) {
					r.add(JSON.parseObject(s, AlarmItem.class));
				}
			}
		} else {
			r = findAllAlarmItemByEs(enable);
		}
		return r;
	}

	@Override
	public AlarmItem saveOrUpdateAlarmItem(AlarmItem alarm) {
		alarm = alarmItemRepository.save(alarm);
		cleanCacheItem();
		return alarm;
	}

	@Override
	public List<T> checkThreshold(List<T> datas, AlarmItem alarm) {
		List<T> r = null;
		if (datas != null && datas.size() > 0 && alarm != null) {
			String key = CACHE_PRX + alarm.getId();
			long t = cache.getExpire(key, TimeUnit.SECONDS);
			if (t > 0) {
				Long v = Long.valueOf(cache.opsForValue().get(key));
				if (alarm.getThreshold() <= v) {
					r = datas;
				}
				cache.opsForValue().set(key, String.valueOf(v + 1), t, TimeUnit.SECONDS);
			} else {
				cache.opsForValue().set(key, "1");
				cache.expire(key, alarm.getThrottle(), TimeUnit.SECONDS);
			}
		}
		return r;
	}

	private void cacheItem(AlarmItem o) {
		if (o != null) {
			cache.boundSetOps(CACHE_ITEM_KEY).add(JSON.toJSONString(o));
		}
	}

	private boolean cleanCacheItem() {
		boolean r = false;
		r = cache.delete(CACHE_ITEM_KEY);
		return r;
	}
}
