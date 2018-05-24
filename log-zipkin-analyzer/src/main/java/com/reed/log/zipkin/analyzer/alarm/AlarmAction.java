package com.reed.log.zipkin.analyzer.alarm;

import com.reed.log.zipkin.analyzer.pojo.BaseObj;

/**
 * 报警动作，Email，SMS
 * @author reed
 *
 */
public class AlarmAction extends BaseObj {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3561406798529643959L;
	
	private String name;
	// 动作类型，Email,SMS
	private String type;
	// 发送者
	private String from;
	// 接收者
	private String to;

	private String subject;

	private String msg;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	
}
