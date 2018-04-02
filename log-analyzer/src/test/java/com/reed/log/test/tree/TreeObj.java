package com.reed.log.test.tree;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * 树形结构
 * @author reed
 *
 */
public class TreeObj implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6730532282828821264L;

	public String id;
	public String name;
	public String parentId;
	public List<TreeObj> childList;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public List<TreeObj> getChildList() {
		return childList;
	}

	public void setChildList(List<TreeObj> childList) {
		this.childList = childList;
	}

	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
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
			for (TreeObj o : this.childList) {
				if (o != null) {
					o.showTree(top + 1);
				}
			}
		}
	}
}