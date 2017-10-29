package com.czp.ulc.coordinator;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * 任务协调器,负责分布式锁任务协调
 *
 * <li>创建人：coder_czp@126.com</li>
 * <li>创建时间：2017年10月29日</li>
 * 
 * @version 0.0.1
 */
public class CoordinatorServer {

	private int port;
	private String bindIp;
	private Selector selector;
	private ServerSocketChannel socketChannel;
	private static final Logger LOG = LoggerFactory.getLogger(CoordinatorServer.class);

	public CoordinatorServer(int port, String bindIp) {
		this.port = port;
		this.bindIp = bindIp;
	}

	public void start() throws IOException {
		InetAddress addr = null;
		if (!Strings.isNullOrEmpty(bindIp)) {
			addr = Inet4Address.getByName(bindIp);
		}
		InetSocketAddress endpoint = new InetSocketAddress(addr, port);
		selector = Selector.open();
		socketChannel = ServerSocketChannel.open();
		socketChannel.socket().setReuseAddress(true);
		socketChannel.socket().bind(endpoint);
		LOG.info("CoordinatorServer listen at:{}", endpoint);
		socketChannel.register(selector, SelectionKey.OP_ACCEPT);
		doDispatch();
	}

	private void doDispatch() throws IOException {
		while (selector.select() > 0) {
			Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
			while (iterator.hasNext()) {
				SelectionKey key = null;
				try {
					key = (SelectionKey) iterator.next();
					iterator.remove();
					if (key.isAcceptable()) {
						doAeecpt(key);
					}
					if (key.isReadable()) {
						doReveice(key);
					}
					if (key.isWritable()) {
						// send(key);
					}
				} catch (Exception e) {
					e.printStackTrace();
					try {
						if (key != null) {
							key.cancel();
							key.channel().close();
						}
					} catch (Exception cex) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void doAeecpt(SelectionKey key) throws Exception {
		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel sc = ssc.accept();
		sc.configureBlocking(false);
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		sc.register(selector, SelectionKey.OP_READ, buffer);
		LOG.info("client:{} connected",sc.socket().getRemoteSocketAddress());
	}

	private void doReveice(SelectionKey key) {

	}

	public static void main(String[] args) {

	}
}
