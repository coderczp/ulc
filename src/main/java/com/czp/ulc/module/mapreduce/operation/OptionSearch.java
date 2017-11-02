package com.czp.ulc.module.mapreduce.operation;

import com.czp.ulc.module.lucene.search.ILocalSearchCallback;
import com.czp.ulc.module.lucene.search.LocalIndexSearcher;
import com.czp.ulc.module.lucene.search.SearchResult;
import com.czp.ulc.module.lucene.search.SearchTask;
import com.czp.ulc.module.mapreduce.IRemoteSearchCallback;
import com.czp.ulc.module.mapreduce.MapReduceTask;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年11月1日</li>
 * 
 * @version 0.0.1
 */

public class OptionSearch implements IOperation {

	@Override
	public String type() {
		return "search";
	}

	@Override
	public void handle(IRemoteSearchCallback rcb, LocalIndexSearcher search, MapReduceTask task) throws Exception {
		SearchTask stask = new SearchTask(task.getQuery());
		stask.setCallback(new ILocalSearchCallback() {

			@Override
			public boolean handle(SearchResult result) {
				return rcb.handle(task.getReqId(), result);
			}

			@Override
			public void finish() {
				rcb.finish(task.getReqId());
			}
		});
		search.localSearch(stask);
	}

}
