package com.reed.log.zipkin.analyzer.alarm;

import java.util.List;

import org.springframework.data.domain.Pageable;

/**
 * 报警服务
 * @author reed
 *
 * @param <T>
 */
public interface AlarmService<T> {
	/**
	 * 校验报警阈值
	 * @param datas
	 * @param alarm
	 * @return
	 */
	public List<T> checkThreshold(List<T> datas, AlarmItem alarm);

	/**
	 * 根据报警条件，获取匹配的数据
	 * @param alarm
	 * @param page
	 * @return
	 */
	public List<T> getDatasByCondition(AlarmItem alarm, Pageable page);

	/**
	 * 根据Id获取报警配置
	 * @param id
	 * @return
	 */
	public AlarmItem findAlarmItemById(Long id);

	/**
	 * 获取全部报警配置
	 * @param enable
	 * @return
	 */
	public List<AlarmItem> findAllAlarmItem(Boolean enable);

	/**
	 * 添加或修改报警配置
	 * @param alarm
	 * @return
	 */
	public AlarmItem saveOrUpdateAlarmItem(AlarmItem alarm);

	/**
	 * 触发报警动作
	 * @param datas
	 * @param alarm
	 */
	public void doActions(List<T> datas, AlarmItem alarm);
}
