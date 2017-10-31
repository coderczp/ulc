package com.czp.ulc.test;

import org.zeromq.ZMQ;

/**
 * 请添加描述
 *
 * <li>创建人：coder_czp@126.com</li>
 * <li>创建时间：2017年10月29日</li>
 * 
 * @version 0.0.1
 */
public class ZmqRepTest {

	public static void main(String[] args) throws InterruptedException {
		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket pub = context.socket(ZMQ.PULL);
		pub.bind("tcp://127.0.0.1:5558");
		while (!Thread.interrupted()) {
			System.out.println("recv："+pub.recvStr());
		}
		pub.close();
		context.term();
	}
}
