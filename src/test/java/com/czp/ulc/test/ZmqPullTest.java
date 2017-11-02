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
		ZMQ.Socket req = context.socket(ZMQ.REQ);
		ZMQ.Socket pull = context.socket(ZMQ.PULL);
		ZMQ.Socket moniter = context.socket(ZMQ.PAIR);

		pub.bind("tcp://*:5556");
		pull.bind("tcp://*:5555");
		
		req.monitor("inproc://reqmoniter", ZMQ.EVENT_CONNECTED | ZMQ.EVENT_DISCONNECTED);
		moniter.connect("inproc://reqmoniter"); 
		
		new Thread(() -> {
			while(!Thread.interrupted()){
				ZMQ.Event event = ZMQ.Event.recv(moniter);
				System.out.println(event.getEvent() +  "->" + event.getAddress());  
			}
		}).start();
		
		while (!Thread.interrupted()) {
			byte[] recv = pull.recv();
			System.out.println("recv message:" + recv);
			pub.send("M".getBytes(), ZMQ.SNDMORE);
			pub.send(recv, 0);
		}
		pub.close();
		context.term();
	}
}
