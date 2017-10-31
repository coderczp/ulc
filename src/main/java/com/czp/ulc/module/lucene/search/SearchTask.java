package com.czp.ulc.module.lucene.search;

import com.czp.ulc.web.QueryCondtion;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月19日 上午8:58:08</li>
 * 
 * @version 0.0.1
 */

public class SearchTask {

	private QueryCondtion query;

	private ILocalSearchCallback callback;

	public ILocalSearchCallback getCallback() {
		return callback;
	}

	public void setCallback(ILocalSearchCallback callback) {
		this.callback = callback;
	}

	public SearchTask(QueryCondtion query) {
		this.query = query;
	}

	public QueryCondtion getQuery() {
		return query;
	}

}
