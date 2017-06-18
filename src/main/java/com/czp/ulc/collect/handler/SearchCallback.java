package com.czp.ulc.collect.handler;

import java.util.HashSet;
import java.util.Set;

import com.czp.ulc.web.QueryCondtion;

/**
 * 请添加描述 <li>创建人：Jeff.cao</li> <li>创建时间：2017年4月19日 上午8:58:08</li>
 * 
 * @version 0.0.1
 */

public class SearchCallback {

	private QueryCondtion query;

	private Set<String> feilds = new HashSet<String>();

	public SearchCallback(QueryCondtion query, String... feild) {
		this.query = query;
		this.addFeild(feild);
	}

	public QueryCondtion getQuery() {
		return query;
	}

	public Set<String> getFeilds() {
		return feilds;
	}

	public void addFeild(String... feild) {
		for (String item : feild) {
			feilds.add(item);
		}
	}

	public boolean handle(String host, String file, String line) {
		return false;
	}

	public void onFinish(long allDoc, long allMatch) {

	}
}
