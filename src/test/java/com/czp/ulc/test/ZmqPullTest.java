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
public class ZmqPullTest {

	public static void main(String[] args) throws InterruptedException {
		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket pub = context.socket(ZMQ.PUB);
		ZMQ.Socket pull = context.socket(ZMQ.PULL);
		pull.bind("tcp://192.168.0.59:5555");
		pub.bind("tcp://192.168.0.59:5556");

		while (!Thread.interrupted()) {
			byte[] recv = pull.recv();
			System.out.println("recv pull:" + recv);
			pub.send("M".getBytes(), ZMQ.SNDMORE);
			pub.send(recv, 0);
		}
		pub.close();
		context.term();
	}
}
