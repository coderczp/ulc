package com.czp.ulc.module.mapreduce;

import com.czp.ulc.module.lucene.search.ILocalSearchCallback;
import com.czp.ulc.module.lucene.search.SearchResult;

/**
 * 这个类将在RPC线程里调用,实际上是另外的进程在调用
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月30日</li>
 * 
 * @version 0.0.1
 */

public class RemoteSearchCallbackImpl implements IRemoteSearchCallback {

	private MapreduceModule mpr;

	public RemoteSearchCallbackImpl(MapreduceModule mpr) {
		this.mpr = mpr;
	}

	@Override
	public boolean handle(long reqId, SearchResult result) {
		ILocalSearchCallback callback = mpr.getMaprequceTask(reqId).getCallback();
		return callback.handle(result);
	}

	@Override
	public void finish(long reqId) {
		// ILocalSearchCallback callback =
		// mpr.getMaprequceTask(reqId).getCallback();
		// callback.finish();
		mpr.cleanTask(reqId);
	}

}
