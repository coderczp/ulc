package com.czp.ulc.module.mapreduce.operation;

import com.czp.ulc.module.lucene.search.LocalIndexSearcher;
import com.czp.ulc.module.mapreduce.IRemoteSearchCallback;
import com.czp.ulc.module.mapreduce.MapReduceTask;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年11月1日</li>
 * 
 * @version 0.0.1
 */

public interface IOperation {

	/***
	 * 处理那种类型的操作
	 * 
	 * @return
	 */
	String type();

	void handle(IRemoteSearchCallback rcb, LocalIndexSearcher search, MapReduceTask task) throws Exception;
}
