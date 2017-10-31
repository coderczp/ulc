package com.czp.ulc.module.mapreduce.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月30日</li>
 * 
 * @version 0.0.1
 */

public class RpcClient {

	private Context namingCtx;

	private static final Logger LOG = LoggerFactory.getLogger(RpcClient.class);

	/** 缓存url对应的IServerCall */
	private ConcurrentHashMap<String, ITransport> callSers = new ConcurrentHashMap<>();

	public RpcClient() {
		try {
			namingCtx = new InitialContext();
		} catch (NamingException e) {
			throw new RuntimeException(e);
		}
	}

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
						callServer = (ITransport) namingCtx.lookup(url);
						callSers.put(url, callServer);
					} catch (NamingException e) {
						throw new RuntimeException("fail to found callser in:" + url, e);
					}
				}
			}
		}
		return callServer;
	}

	public boolean stop() {
		try {
			callSers.clear();
			namingCtx.close();
		} catch (NamingException e) {
			LOG.error("rpc client close err", e);
		}
		return true;
	}

}
