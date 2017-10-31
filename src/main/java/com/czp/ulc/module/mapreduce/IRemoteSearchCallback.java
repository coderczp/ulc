package com.czp.ulc.module.mapreduce;

import com.czp.ulc.module.lucene.search.SearchResult;

/**
 * 这个类将在RPC线程里调用,实际上是另外的进程在调用
 * 
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月30日</li>
 * 
 * @version 0.0.1
 */
public interface IRemoteSearchCallback {

	boolean handle(long reqId, SearchResult result);

	void finish(long reqId);
}
