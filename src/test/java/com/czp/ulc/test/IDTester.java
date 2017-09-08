/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.test;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.common.util.IdGnerator;

/**
 * Function:XXX TODO add desc
 *
 * @date:2017年3月30日/上午11:54:38
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class IDTester {

	public static void main(String[] args) {
		Enumeration<org.apache.log4j.Logger> allAppenders = LogManager.getCurrentLoggers();
	    while(allAppenders.hasMoreElements()){
	    	System.out.println("-------->");
	    	PatternLayout ot = new PatternLayout("yyyy_MM-dd");
	    	allAppenders.nextElement().getAppender("console").setLayout(ot);
	    }	
	    Logger logger = LoggerFactory.getLogger("test");
	    logger.info("000000");
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
