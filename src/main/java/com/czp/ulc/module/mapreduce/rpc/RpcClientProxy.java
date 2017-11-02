package com.czp.ulc.module.mapreduce.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Naming;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月30日</li>
 * 
 * @version 0.0.1
 */

public class RpcClientProxy {

	@SuppressWarnings("unchecked")
	public <T> T getServer(String url, Class<T> itf) throws Exception {
		ITransport callSer = findTransport(url);
		return (T) Proxy.newProxyInstance(itf.getClassLoader(), new Class[] { itf }, new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				String beanId = itf.getName();
				return callSer.call(beanId, TransportImpl.buildKey(method), args);
			}
		});
	}

	private ITransport findTransport(String url) throws Exception {
		return (ITransport) Naming.lookup(url);
	}
}
