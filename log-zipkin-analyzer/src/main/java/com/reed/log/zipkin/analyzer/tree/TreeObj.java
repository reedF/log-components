package com.reed.log.zipkin.analyzer.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 树形结构
 * @author reed
 *
 */
public class TreeObj<T> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6730532282828821264L;

	public String id;
	public String app;
	public String name;
	public String parentId;
	public T biz;
	//jeasyui-datagrid解析json格式要求
	@JsonProperty(value = "children")
	public List<TreeObj<T>> childList;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getApp() {
		return app;
	}

	public void setApp(String app) {
		this.app = app;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public T getBiz() {
		return biz;
	}

	public void setBiz(T biz) {
		this.biz = biz;
	}

	public List<TreeObj<T>> getChildList() {
		return childList;
	}

	public void setChildList(List<TreeObj<T>> childList) {
		this.childList = childList;
	}

	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj, new String[] { "biz" });
	}

	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this, new String[] { "biz" });
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
	}

	public void showTree(int top) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < top; i++) {
			sb.append("-");
		}
		System.out.println(sb.toString() + this.id + "===" + this.name);
		if (this.childList != null) {
			for (TreeObj<T> o : this.childList) {
				if (o != null) {
					o.showTree(top + 1);
				}
			}
		}
	}

	/**
	 * 返回树各层级节点，已排序
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<String> showAllNodes() {
		List<String> r = new ArrayList<>();
		if (this != null) {
			r.add(this.name);
			if (this.getChildList() != null && !this.getChildList().isEmpty()) {
				for (TreeObj t : this.getChildList()) {
					r.addAll(t.showAllNodes());
				}
			}
		}
		return r;
	}
}