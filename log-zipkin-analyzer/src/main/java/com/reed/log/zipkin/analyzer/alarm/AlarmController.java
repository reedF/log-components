package com.reed.log.zipkin.analyzer.alarm;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reed.log.zipkin.analyzer.es.EsZipkin;

/**
 * 报警控制
 * @author reed
 *
 */
@RestController
@RequestMapping("/alarm")
public class AlarmController {

	@Autowired
	private AlarmEsDataSourceService alarmEsService;

	@Autowired
	private AlarmItemRepository alarmRespository;

	/**
	 * 分页查询
	 * @param enable 是否启用，非必传
	 * @param pageable 分页，参数名：page：当前页数，size：当前页容量，sort:排序字段（格式："字段1,字段2,...,DESC|ASC"）
	 * @return
	 */
	@RequestMapping(value = "/list")
	public ResponseEntity<Page<AlarmItem>> listAlarm(Boolean enable,
			@PageableDefault(size = 20, sort = { "id" }, direction = Direction.ASC) Pageable pageable) {
		ResponseEntity<Page<AlarmItem>> r = new ResponseEntity<>(HttpStatus.OK);
		Page<AlarmItem> page = alarmRespository.findByEnable(enable, pageable);
		Page<AlarmItem> pageResult = new PageImpl<>(page.getContent(), pageable, page.getTotalElements());
		r = ResponseEntity.ok().body(pageResult);
		return r;
	}

	@RequestMapping(value = "/dist")
	public ResponseEntity<Page<AlarmItem>> listDistAlarm(String title,
			@PageableDefault(size = 20, sort = { "id" }, direction = Direction.ASC) Pageable pageable) {
		ResponseEntity<Page<AlarmItem>> r = new ResponseEntity<>(HttpStatus.OK);
		Page<AlarmItem> page = alarmEsService.findDistinctByTitle(title, pageable);
		Page<AlarmItem> pageResult = new PageImpl<>(page.getContent(), pageable, page.getTotalElements());
		r = ResponseEntity.ok().body(pageResult);
		return r;
	}

	/**
	 * 根据Id获取报警配置
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/get")
	public ResponseEntity<AlarmItem> getAlarmItem(Long id) {
		ResponseEntity<AlarmItem> r = new ResponseEntity<>(HttpStatus.OK);
		r = ResponseEntity.ok().body(alarmEsService.findAlarmItemById(id));
		return r;
	}

	/**
	 * 添加或修改报警配置
	 * @param alarm
	 * @return
	 */
	@RequestMapping(value = "/edit")
	public ResponseEntity<AlarmItem> saveOrUpdateAlarmItem(@RequestBody AlarmItem alarm) {
		ResponseEntity<AlarmItem> r = new ResponseEntity<>(HttpStatus.OK);
		r = ResponseEntity.ok().body(alarmEsService.saveOrUpdateAlarmItem(alarm));
		return r;
	}

	/**
	 * 测试报警条件
	 * @param id
	 * @param pageable
	 * @return
	 */
	@RequestMapping(value = "/test")
	public ResponseEntity<List<EsZipkin>> testAlarmCondition(Long id,
			@PageableDefault(size = 20, sort = { "createTime" }, direction = Direction.DESC) Pageable pageable) {
		ResponseEntity<List<EsZipkin>> r = new ResponseEntity<>(HttpStatus.OK);
		AlarmItem a = alarmEsService.findAlarmItemById(id);
		r = ResponseEntity.ok().body(alarmEsService.getDatasByCondition(a, pageable));
		return r;
	}

}
