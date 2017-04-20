package com.czp.ulc.common;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Function:线程池
 *
 * @date:2016年10月19日/下午6:48:43
 * @Author:coder_czp@126.com
 * @version:1.0
 */
public class ThreadPools {

	private static final Logger LOG = LoggerFactory.getLogger(ThreadPools.class);

	private static final ThreadPools INSTANCE = new ThreadPools();

	private List<Thread> threads = new CopyOnWriteArrayList<Thread>();

	private ThreadPools() {
	}

	public static ThreadPools getInstance() {
		return INSTANCE;
	}

	/***
	 * 创建新线程
	 * 
	 * @param name
	 *            线程名称
	 * @param task
	 *            要执行的任务
	 * @return
	 */
	public void startThread(String name, Runnable task,boolean daemon) {
		Thread t = new Thread(task, name);
		t.setDaemon(daemon);
		threads.add(t);
		t.start();
		LOG.debug("create thread:{} task:{}", name, task);
	}
}
