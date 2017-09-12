package com.czp.ulc.util;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * 字典树 <li>创建人：Jeff.cao</li> <li>创建时间：2017年4月1日 上午10:30:32</li>
 * 
 * @version 0.0.1
 */

public class TrieTree {

	/* 树节点,支持次数统计 */
	static class TreeNode {
		/* 字符 */
		char ch;
		/* 是否是根节点 */
		boolean isEnd;
		/* 子节点 */
		HashMap<Character, TreeNode> childs = new HashMap<Character, TreeNode>();

		BitSet map = new BitSet();

		public TreeNode(char c) {
			ch = c;
		}
	}

	private TreeNode root = new TreeNode(' ');

	/**
	 * 插入字符串
	 * 
	 * @param str
	 */
	public void put(String str) {
		TreeNode t = root;
		int len = str.length();
		BitSet map = root.map;
		HashMap<Character, TreeNode> children = root.childs;
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			if (!map.get(c)) {
				t = new TreeNode(c);
				children.put(c, t);
				map.set(c);
			} else {
				t = children.get(c);
			}
			children = t.childs;
			map = t.map;
		}
		t.isEnd = true;

	}

	/**
	 * 是否包含目标字符串
	 * 
	 * @param str
	 * @return
	 */
	public boolean contains(String str) {
		if (str == null)
			return false;
		TreeNode t = searchNode(str);
		if (t != null && t.isEnd)
			return true;
		else
			return false;
	}

	/***
	 * 是否已字符串开头
	 * 
	 * @param str
	 * @return
	 */
	public boolean startWith(String str) {
		if (str == null)
			return false;
		return searchNode(str) != null;
	}

	public TreeNode searchNode(String str) {
		TreeNode t = null;
		int len = str.length();
		BitSet map = root.map;
		Map<Character, TreeNode> children = root.childs;
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			if (map.get(c)) {
				t = children.get(c);
				children = t.childs;
				map = t.map;
			} else {
				return null;
			}
		}
		return t;
	}

}
