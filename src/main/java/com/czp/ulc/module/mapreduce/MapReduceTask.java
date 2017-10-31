package com.czp.ulc.module.mapreduce;

import java.io.Serializable;

import com.czp.ulc.web.QueryCondtion;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月30日</li>
 * 
 * @version 0.0.1
 */

public class MapReduceTask implements Serializable {

	private static final long serialVersionUID = 1L;

	/** 发起mapreduce的机器将回调的rpcurl暴露出去 */
	private String rpcUrl;

	/** 查询条件 */
	private QueryCondtion query;

	/***
	 * 保证在当前进程内唯一即可
	 */
	private long reqId;
	
	public void setReqId(long reqId) {
		this.reqId = reqId;
	}

	public String getRpcUrl() {
		return rpcUrl;
	}

	public void setRpcUrl(String rpcUrl) {
		this.rpcUrl = rpcUrl;
	}

	public QueryCondtion getQuery() {
		return query;
	}

	public void setQuery(QueryCondtion query) {
		this.query = query;
	}

	public long getReqId() {
		return reqId;
	}

}
