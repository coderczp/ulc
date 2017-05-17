/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.common;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.common.shutdown.ShutdownCallback;
import com.czp.ulc.common.shutdown.ShutdownManager;
import com.czp.ulc.common.util.Utils;

/**
 * Function:消息派发中心
 *
 * @date:2017年3月22日/下午3:52:54
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
@SuppressWarnings("rawtypes")
public class MessageCenter implements ShutdownCallback, Runnable {

	private volatile boolean stop;

	private BlockingQueue<Message> tasks;

	private Map<Class, List<MessageListener>> concumners;

	private static final MessageCenter INSTANCE = new MessageCenter();

	private static Logger LOG = LoggerFactory.getLogger(MessageCenter.class);

	private MessageCenter() {
		tasks = new LinkedBlockingQueue<Message>(2000);
		concumners = new ConcurrentHashMap<Class, List<MessageListener>>();

		ShutdownManager.getInstance().addCallback(this);

		int maxWorkSize = 10;
		for (int i = 0; i < maxWorkSize; i++) {
			ThreadPools.getInstance().startThread("message-worker" + i, this, true);
		}

	}

	public static MessageCenter getInstance() {
		return INSTANCE;
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
	public void onSystemExit() {
		stop = true;
		for (Entry<Class, List<MessageListener>> work : concumners.entrySet()) {
			for (MessageListener item : work.getValue()) {
				item.onExit();
			}
		}
		LOG.info("all consumer exit has been called");
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run() {
		while (!stop) {
			try {
				Message message = tasks.take();
				List<MessageListener> list = concumners.get(message.getMessage().getClass());
				if (Utils.isEmpty(list)) {
					LOG.error("can't find processor for:{}", message.getMessage());
				}
				for (MessageListener listener : list) {
					listener.onMessage(message.getMessage(), message.getExt());
				}
			} catch (Exception e) {
				LOG.error("handle messaeg error", e);
			}
		}
	}
}
