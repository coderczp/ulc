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
public class ZmqTest {

	public static void main(String[] args) throws InterruptedException {
		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket pub = context.socket(ZMQ.PUB);
		pub.bind("tcp://*:5558");

		Scanner sc = new Scanner(System.in);
		while (!Thread.interrupted()) {
			System.out.println("Enter info");
			String line = sc.nextLine();
			pub.send("B".getBytes(), ZMQ.SNDMORE);
			pub.send(line.getBytes(),0);
		}
		sc.close();
		pub.close();
		context.term();
	}
}
