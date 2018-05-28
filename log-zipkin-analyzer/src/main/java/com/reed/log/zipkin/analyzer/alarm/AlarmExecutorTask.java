package com.reed.log.zipkin.analyzer.alarm;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.reed.log.zipkin.analyzer.es.EsZipkin;

@Component
public class AlarmExecutorTask {

	private static Logger logger = LoggerFactory.getLogger(AlarmExecutorTask.class);
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	// 执行频率缓存
	private Map<Long, Long> frequencyMap = new ConcurrentHashMap<>();

	@Autowired
	private AlarmEsDataSourceService alarmEsService;

	@Scheduled(cron = "${alarm.schedule}")
	public void executeAlarm() {
		try {
			int size = 0;
			List<AlarmItem> alarms = alarmEsService.findAllAlarmItem(true);
			if (alarms != null && alarms.size() > 0) {
				cleanDisableItem(alarms);
				size = alarms.size();
				Pageable pageable = PageRequest.of(0, 20, Direction.DESC, "createTime");
				for (AlarmItem a : alarms) {
					if (a != null && checkRun(a)) {
						List<EsZipkin> datas = alarmEsService.getDatasByCondition(a, pageable);
						datas = alarmEsService.checkThreshold(datas, a);
						if (datas != null && datas.size() > 0) {
							alarmEsService.doActions(datas, a);
						}
					}
				}
			}
			logger.info("=========Alarm task done in current time : {},item Num is:{} =========",
					sdf.format(new Date()), size);
		} catch (Exception e) {
			logger.error("=========Alarm task ERROR:{},{} ==========", e.getMessage(), e.getCause());
		}
	}

	private boolean checkRun(AlarmItem a) {
		boolean r = false;
		long currentTime = System.currentTimeMillis() / 1000;
		long nextRunTime = currentTime + a.getFrequency();
		if (!frequencyMap.containsKey(a.getId())) {
			r = true;
		} else {
			Long runTime = frequencyMap.get(a.getId());
			if (currentTime >= runTime) {
				r = true;
			}
		}
		if (r) {
			frequencyMap.put(a.getId(), nextRunTime);
		}

		return r;
	}

	private void cleanDisableItem(List<AlarmItem> alarms) {
		Set<Long> ids = new HashSet<>();
		Set<Long> needClean = new HashSet<>(frequencyMap.keySet());
		alarms.forEach(a -> ids.add(a.getId()));
		needClean.removeAll(ids);
		Iterator<Long> iter = frequencyMap.keySet().iterator();
		while (iter.hasNext()) {
			Long key = iter.next();
			if (needClean.contains(key)) {
				// java.util.ConcurrentModificationException
				// frequencyMap.remove(key);
				iter.remove(); // OK
			}
		}
	}
}
