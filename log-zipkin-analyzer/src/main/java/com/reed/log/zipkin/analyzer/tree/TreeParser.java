package com.reed.log.zipkin.analyzer.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析树形数据工具类
 * 
 */
@SuppressWarnings("rawtypes")
public class TreeParser {
	/**
	 * 解析树形数据
	 * @param topId
	 * @param entityList
	 * @return
	 */

	public static List<TreeObj> getTreeList(String topId, List<TreeObj> entityList) {
		List<TreeObj> resultList = new ArrayList<>();

		// 获取顶层元素集合
		String parentId;
		for (TreeObj entity : entityList) {
			parentId = entity.getParentId();
			if (parentId == null || topId.equals(parentId)) {
				resultList.add(entity);
			}
		}

		// 获取每个顶层元素的子数据集合
		for (TreeObj entity : resultList) {
			entity.setChildList(getSubList(entity.getId(), entityList));
		}

		return resultList;
	}

	/**
	 * 获取子数据集合
	 * @param id
	 * @param entityList
	 * @return
	 */
	private static List<TreeObj> getSubList(String id, List<TreeObj> entityList) {
		List<TreeObj> childList = new ArrayList<>();
		String parentId;

		// 子集的直接子对象
		for (TreeObj entity : entityList) {
			parentId = entity.getParentId();
			if (id.equals(parentId)) {
				childList.add(entity);
			}
		}

		// 子集的间接子对象
		for (TreeObj entity : childList) {
			entity.setChildList(getSubList(entity.getId(), entityList));
		}

		// 递归退出条件
		if (childList.size() == 0) {
			return null;
		}

		return childList;
	}

	public static void main(String[] args) {
		List<TreeObj> list = new ArrayList<>();
		TreeObj menu = new TreeObj();
		menu.setId("0");
		menu.setParentId("-1");
		menu.setName("菜单");
		list.add(menu);

		TreeObj menu1 = new TreeObj();
		menu1.setId("1");
		menu1.setParentId("0");
		menu1.setName("菜单1");
		list.add(menu1);

		TreeObj menu2 = new TreeObj();
		menu2.setId("2");
		menu2.setParentId("0");
		menu2.setName("菜单2");
		list.add(menu2);

		TreeObj menu3 = new TreeObj();
		menu3.setId("3");
		menu3.setParentId("1");
		menu3.setName("菜单3");
		list.add(menu3);

		TreeObj menu4 = new TreeObj();
		menu4.setId("4");
		menu4.setParentId("3");
		menu4.setName("菜单4");
		list.add(menu4);

		TreeObj menu5 = new TreeObj();
		menu5.setId("5");
		menu5.setParentId("3");
		menu5.setName("菜单5");
		list.add(menu5);

		TreeObj menu6 = new TreeObj();
		menu6.setId("6");
		menu6.setParentId("-1");
		menu6.setName("菜单6");
		list.add(menu6);

		menu.showTree(0);

		List<TreeObj> menus = TreeParser.getTreeList("-1", list);
		System.out.println(menus);

		menu.showTree(0);
		menu2.showTree(0);

		System.out.println(menu1.equals(menu6));
		System.out.println(menu);
	}
}
