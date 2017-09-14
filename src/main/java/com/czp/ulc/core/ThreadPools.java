package com.czp.ulc.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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

	private static class NameThreadFactory implements ThreadFactory {

		String name;
		boolean daemon;

		public NameThreadFactory(String name, boolean daemon) {
			this.name = name;
			this.daemon = daemon;
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread th = new Thread(r);
			th.setDaemon(daemon);
			th.setName(name);
			return th;
		}

	}

	private static final Logger LOG = LoggerFactory.getLogger(ThreadPools.class);

	private static final ThreadPools INSTANCE = new ThreadPools();

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
	public void run(String name, Runnable task, boolean daemon) {
		Thread t = new Thread(task, name);
		t.setDaemon(daemon);
		t.start();
		LOG.debug("create thread:{} task:{}", name, task);
	}

	/***
	 * 创建线程池
	 * @param name
	 * @param threadSize
	 * @return
	 */
	public ExecutorService newPool(String name, int threadSize) {
		return Executors.newFixedThreadPool(threadSize, new NameThreadFactory(name, true));
	}
}
