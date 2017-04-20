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

import java.nio.ByteBuffer;
import java.util.Date;
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

	/****
	 * 从2017年4月1号计数
	 */
	@SuppressWarnings("deprecation")
	private long base = new Date(2017 - 1900, 4 - 1, 1).getTime();

	// 自增ID
	private AtomicInteger autoIncrId = new AtomicInteger();

	// 上次产生的时间
	private long lastTime;

	public static IdGnerator getInstance() {
		return INSTANCE;
	}

	/**
	 * 动态编码的byteID,每毫秒支持20亿ID,最大8字节时间戳,4字节递增ID<br>
	 * 
	 * @param now
	 * @return
	 */
	public byte[] nextBytesId(long now) {

		if (lastTime != now)
			autoIncrId.set(0);

		int id = autoIncrId.getAndIncrement();
		if (id < 0) {
			// 每毫秒请求超过20亿次,自增ID溢出
			throw new RuntimeException("autoIncrId is over flow");
		}

		long time = now - base;
		boolean isIntTime = time < Integer.MAX_VALUE;
		int bufSize = isIntTime ? 4 : 8;// 要申请的内存大小
		if (id < Byte.MAX_VALUE) {
			bufSize += 1;
		} else if (id < Character.MAX_VALUE) {
			bufSize += 2;
		} else {
			bufSize += 4;
		}

		ByteBuffer buf = ByteBuffer.allocate(bufSize);
		if (isIntTime) {
			buf.putInt((int) time);
		} else {
			buf.putLong(time);
		}

		if (id < Byte.MAX_VALUE) {
			buf.put((byte) id);
		} else if (id < Character.MAX_VALUE) {
			buf.putChar((char) id);
		} else {
			buf.putInt(id);
		}

		lastTime = now;
		return buf.array();
	}

	/**
	 * 8字节时间戳,4字节递增ID
	 * 
	 * @param now
	 * @return
	 */
	public byte[] nextBytesId() {
		return nextBytesId(System.currentTimeMillis());
	}
}
