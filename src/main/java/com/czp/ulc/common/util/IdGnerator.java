/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.common.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Function:ID生成器
 *
 * @date:2017年3月29日/下午12:36:59
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class IdGnerator {

	private static final IdGnerator INSTANCE = new IdGnerator();

	// 自增ID
	private AtomicInteger autoIncrId = new AtomicInteger();

	// 上次产生的时间
	private long lastTime;

	private int lastId;

	public static IdGnerator getInstance() {
		return INSTANCE;
	}

	/**
	 * 8字节时间戳,4字节递增ID 每毫秒支持21亿ID<br>
	 * 
	 * @param now
	 * @return
	 */
	public int nextId(long now) {

		if (lastTime != now)
			autoIncrId.compareAndSet(lastId, 0);

		int id = autoIncrId.getAndIncrement();
		if (id < 0) {
			// 每毫秒请求超过20亿次,自增ID溢出
			throw new RuntimeException("autoIncrId is over flow");
		}
		lastTime = now;
		lastId = id;
		return id;
	}
}
