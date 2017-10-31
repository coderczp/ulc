package com.czp.ulc.module.mapreduce.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Naming;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月30日</li>
 * 
 * @version 0.0.1
 */

public class RpcClientProxy {

	/** 缓存url对应的IServerCall */
	private ConcurrentHashMap<String, ITransport> callSers = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public <T> T getServer(String url, Class<T> itf) {
		ITransport callSer = findTransport(url);
		return (T) Proxy.newProxyInstance(itf.getClassLoader(), new Class[] { itf }, new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				String beanId = itf.getName();
				return callSer.call(beanId, TransportImpl.buildKey(method), args);
			}
		});
	}

	private ITransport findTransport(String url) {
		ITransport callServer = callSers.get(url);
		if (callServer == null) {
			synchronized (callSers) {
				if (callServer == null) {
					try {
						callServer = (ITransport) Naming.lookup(url);
						callSers.put(url, callServer);
					} catch (Exception e) {
						throw new RuntimeException("fail to found rmi ser in:" + url, e);
					}
				}
			}
		}
		return callServer;
	}
}
