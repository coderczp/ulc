package com.czp.ulc.module.mapreduce.rpc;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月30日</li>
 * 
 * @version 0.0.1
 */

public class TransportImpl extends UnicastRemoteObject implements ITransport {

	private static final long serialVersionUID = 1L;

	/** 本地服务表key为beanId */
	private ConcurrentHashMap<String, Object> servers = new ConcurrentHashMap<>();

	/** 本地方法表key为beanId+method+argType */
	private ConcurrentHashMap<String, Method> methods = new ConcurrentHashMap<>();

	public TransportImpl() throws RemoteException {
		super();
	}

	/***
	 * 导出本地本地服务为RPC服务
	 * 
	 * @param beanId
	 * @param server
	 * @return
	 */
	public Object export(String beanId, Object server) {
		servers.put(beanId, server);
		Class<?> cls = server.getClass();
		Method[] dMethods = cls.getDeclaredMethods();
		for (Method method : dMethods) {
			methods.put(buildKey(method), method);
		}
		return server;
	}

	public static String buildKey(Method method) {
		String string = method.toString();
		// (long,com.czp.ulc.module.lucene.search.SearchResult)
		String param = string.substring(string.lastIndexOf("("), string.lastIndexOf(")"));
		return method.getName().concat(param);
	}

	@Override
	public Object call(String beanId, String methodName, Object[] args) throws Exception {
		Object object = servers.get(beanId);
		if (object == null) {
			throw new RuntimeException(beanId + " not found");
		}
		Method method = methods.get(methodName);
		if (method == null)
			throw new RuntimeException(methodName + " not found");

		return method.invoke(object, args);
	}

}
