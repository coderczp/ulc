package com.czp.ulc.module.conn;

/**
 * 执行命令结果回调
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月11日</li>
 * 
 * @version 0.0.1
 */

public interface IExeCallBack {

	void onResponse(String line);
	
	void onError(String err);
}
