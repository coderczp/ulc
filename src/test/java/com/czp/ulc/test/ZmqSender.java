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
public class ZmqSender {

	public static void main(String[] args) throws InterruptedException {
		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket receiver = context.socket(ZMQ.SUB);
		receiver.connect("tcp://127.0.0.1:5558");
		receiver.subscribe("B".getBytes());    
		System.out.println("wait message");
		while(!Thread.interrupted()) {
			byte[] recv = receiver.recv(0);
			System.out.println(new String(recv));
		}
		receiver.close();
		context.term();
	}
}
