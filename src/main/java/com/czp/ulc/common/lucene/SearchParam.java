/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.common.lucene;

import org.apache.lucene.search.IndexSearcher;

import com.czp.ulc.collect.handler.SearchCallback;

/**
 * Function:搜索参数封装
 *
 * @date:2017年5月27日/下午2:45:35
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class SearchParam {

	private long docs;
	private String host;
	private IndexSearcher searcher;
	private SearchCallback callBack;

	public SearchParam(long docs, String host, IndexSearcher searcher, SearchCallback callBack) {
		this.host = host;
		this.searcher = searcher;
		this.callBack = callBack;
	}

	public long getDocs() {
		return docs;
	}

	public String getHost() {
		return host;
	}

	public SearchCallback getCallBack() {
		return callBack;
	}

	public IndexSearcher getSearcher() {
		return searcher;
	}

}
