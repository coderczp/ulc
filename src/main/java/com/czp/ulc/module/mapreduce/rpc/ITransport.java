package com.czp.ulc.module.mapreduce.rpc;

import java.rmi.Remote;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月30日</li>
 * 
 * @version 0.0.1
 */

public interface ITransport extends Remote{

	Object call(String beanId, String methodName, Object[] args) throws Exception;
}
