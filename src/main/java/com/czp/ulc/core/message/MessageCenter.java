/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.core.message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.czp.ulc.core.ThreadPools;
import com.czp.ulc.module.IModule;
import com.czp.ulc.util.Utils;

/**
 * Function:消息派发中心
 *
 * @date:2017年3月22日/下午3:52:54
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
@Service
@SuppressWarnings("rawtypes")
public class MessageCenter implements IModule, Runnable {

	@Autowired
	private Environment env;

	private volatile boolean stop;

	private BlockingQueue<Message> tasks;

	private Map<Class, List<MessageListener>> concumners;

	private static Logger LOG = LoggerFactory.getLogger(MessageCenter.class);

	@PostConstruct
	public void init() {
		int mqThreadSize = Integer.valueOf(env.getProperty("mq.thread.size", "4"));
		int mqQueueSize = Integer.valueOf(env.getProperty("mq.queue.size", "2000"));

		tasks = new LinkedBlockingQueue<Message>(mqQueueSize);
		concumners = new ConcurrentHashMap<Class, List<MessageListener>>();

		for (int i = 0; i < mqThreadSize; i++) {
			ThreadPools.getInstance().run("message-worker" + i, this, true);
		}
	}

	/***
	 * 添加消费者
	 * 
	 * @param listener
	 * @return
	 */
	public boolean addConcumer(MessageListener listener) {
		List<MessageListener> list = concumners.get(listener.processClass());
		if (list == null) {
			concumners.put(listener.processClass(), new CopyOnWriteArrayList<MessageListener>());
		}
		LOG.info("add concumer {},count:{}", listener, concumners.size());
		return concumners.get(listener.processClass()).add(listener);
	}

	/**
	 * 发布消息
	 * 
	 * @param message
	 *            消息对象
	 * @param ext
	 *            扩展参数
	 * @return
	 */
	public boolean push(Message message) {
		try {
			tasks.put(message);
		} catch (InterruptedException e) {
			LOG.error("push messaeg error", e);
		}
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run() {
		while (!stop) {
			try {
				Message message = tasks.take();
				Object body = message.getMessage();
				List<MessageListener> list = concumners.get(body.getClass());
				if (Utils.isEmpty(list)) {
					LOG.error("can't find processor for:{}", body);
				}
				for (MessageListener listener : list) {
					listener.onMessage(body, message.getExt());
				}
			} catch (Exception e) {
				LOG.error("handle messaeg error", e);
			}
		}
	}

	@Override
	public boolean start(SingletonBeanRegistry ctx) {
		return true;
	}

	@Override
	@PreDestroy
	public synchronized boolean stop() {
		stop = true;
		concumners.values().forEach(it -> it.forEach(item -> item.onExit()));
		LOG.info("all consumer exit has been called");
		concumners.clear();
		return true;
	}

	@Override
	public String name() {
		return "MQ Moudle";
	}

	@Override
	public int order() {
		return -1;
	}
}
