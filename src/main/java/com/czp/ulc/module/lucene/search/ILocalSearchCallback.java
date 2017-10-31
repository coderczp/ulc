package com.czp.ulc.module.lucene.search;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月31日</li>
 * 
 * @version 0.0.1
 */

public interface ILocalSearchCallback {

	boolean handle(SearchResult result);

	void finish();
}
