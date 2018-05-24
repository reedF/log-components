package com.reed.log.zipkin.analyzer.alarm;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.reed.log.zipkin.analyzer.pojo.BaseObj;

/**
 * 报警项配置信息
 * @author reed
 *
 */
@Document(indexName = "alarm-config", type = "alarm")
public class AlarmItem extends BaseObj {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8060564517234537334L;

	@Id
	private Long id = System.currentTimeMillis();

	private String title;

	private String description;

	private boolean enable;
	// 报警元数据源类型，ES、DB
	private String dataSourceType;
	// 数据源，ES:索引名;DB:数据库名
	private String dataSource;
	// 执行频率，单位：秒
	private long frequency;
	// 有效期(必须大于frequency)，单位：秒
	private long throttle;
	// 阈值，Throttle有效期内超出累计次数才报警
	private long threshold;

	// 执行条件，ES可直接使用Query DSL；DB可直接使用SQL
	//ES-example:{"term":{"type.keyword":"COST"}},{"term":{"app.keyword":"shop"}},{ "range" : { "children.duration" : {"from": 5000000}}}
	private String condition;
	// 触发操作，Email,SMS等
	@Field(type = FieldType.Object)
	private Set<AlarmAction> actions;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public String getDataSourceType() {
		return dataSourceType;
	}

	public void setDataSourceType(String dataSourceType) {
		this.dataSourceType = dataSourceType;
	}

	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public long getFrequency() {
		return frequency;
	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	public long getThrottle() {
		return throttle;
	}

	public void setThrottle(long throttle) {
		this.throttle = throttle;
	}

	public long getThreshold() {
		return threshold;
	}

	public void setThreshold(long threshold) {
		this.threshold = threshold;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public Set<AlarmAction> getActions() {
		return actions;
	}

	public void setActions(Set<AlarmAction> actions) {
		this.actions = actions;
	}

}
