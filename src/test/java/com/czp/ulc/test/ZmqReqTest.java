package com.czp.ulc.test;

import java.util.Scanner;

import org.zeromq.ZMQ;

/**
 * 请添加描述
 *
 * <li>创建人：coder_czp@126.com</li>
 * <li>创建时间：2017年10月29日</li>
 * 
 * @version 0.0.1
 */
public class ZmqReqTest {

	public static void main(String[] args) throws InterruptedException {
		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket pub = context.socket(ZMQ.PUSH);
		pub.connect("tcp://127.0.0.1:5555");
		boolean send = pub.send("test");
		System.out.println(send);
		pub.setLinger(0);
		pub.close();
		System.out.println("-------------->");
		context.term();
		System.out.println("--------xx------>");
	}
}
