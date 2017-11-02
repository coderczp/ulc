package com.czp.ulc.coordinator;

import org.zeromq.ZMQ;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年11月1日</li>
 * 
 * @version 0.0.1
 */
public class ZMQLoadbalance {

	public static void main(String[] args) throws InterruptedException {
		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket pub = context.socket(ZMQ.PUB);
		ZMQ.Socket pull = context.socket(ZMQ.PUSH);

		pub.bind("tcp://*:5000");
		pull.bind("tcp://*:5001");

		byte[] topic = "X".getBytes();
		while (!Thread.interrupted()) {
			byte[] recv = pull.recv();
			System.out.println("recv message:" + recv);
			pub.send(topic, ZMQ.SNDMORE);
			pub.send(recv, 0);
		}
		pub.close();
		pull.close();
		context.term();
	}
}
