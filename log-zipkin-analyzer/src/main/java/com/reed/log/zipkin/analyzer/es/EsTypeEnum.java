package com.reed.log.zipkin.analyzer.es;

/**
 * es中存储zipkin的类型
 * @author reed
 *
 */
public enum EsTypeEnum {
	COST("COST", "耗时"), ERROR("ERROR", "错误");

	private final String name;

	private final String des;

	private EsTypeEnum(String name, String des) {
		this.name = name;
		this.des = des;
	}

	public String getName() {
		return name;
	}

	public String getDes() {
		return des;
	}

}
