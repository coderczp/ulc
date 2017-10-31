package com.czp.ulc.test;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import zmq.SocketBase;
import zmq.ZError;

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
		pull.bind("tcp://*:5555");
		pub.bind("tcp://*:5556");

		new Thread(() -> {
			int events = 0;
			Socket s = context.socket(ZMQ.PAIR);
			s.connect("tcp://*:5555");
			while (!Thread.interrupted()) {
				ZMQ.Event event = ZMQ.Event.recv(s);
				System.out.println("Unkown Event " + event);
				if (event == null && s.errno() == ZError.ETERM) {
					break;
				}
				switch (event.getEvent()) {
				// listener specific
				case ZMQ.EVENT_LISTENING:
				case ZMQ.EVENT_ACCEPTED:
					// connecter specific
				case ZMQ.EVENT_CONNECTED:
					break;
				case ZMQ.EVENT_CONNECT_DELAYED:
					// generic - either end of the socket
				case ZMQ.EVENT_CLOSE_FAILED:
				case ZMQ.EVENT_CLOSED:
				case ZMQ.EVENT_DISCONNECTED:
				case ZMQ.EVENT_MONITOR_STOPPED:
					events |= event.getEvent();
					break;
				default:
					// out of band / unexpected event
					System.out.println("Unkown Event " + event.getEvent());
				}
			}
			s.close();
		}).start();
		;
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
