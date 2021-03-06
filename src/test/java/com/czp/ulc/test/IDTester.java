package com.czp.ulc.test;
/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */


import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.czp.ulc.util.IdGnerator;

/**
 * Function:XXX TODO add desc
 *
 * @date:2017年3月30日/上午11:54:38
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class IDTester {

	public static void main(String[] args) {
         System.out.println(new Date(1504886400000L).toLocaleString());

	}
	
	@Test
	public void test() throws InterruptedException {
		int count = 50;
		final CountDownLatch lock = new CountDownLatch(count);
		final IdGnerator instance = IdGnerator.getInstance();
		final ConcurrentHashMap<Long, Boolean> map = new ConcurrentHashMap<>();
		for (int i = 0; i < count; i++) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					for (int i = 0; i < 100000; i++) {
						long id = instance.nextId(System.currentTimeMillis());
						if (map.get(id) != null) {
							System.out.println(map.get(id));
							throw new RuntimeException("repeat");
						}
						map.put(id, true);
					}
					lock.countDown();
					System.out.println(map.size());
				}
			}).start();
		}
		lock.await();
	}
}
