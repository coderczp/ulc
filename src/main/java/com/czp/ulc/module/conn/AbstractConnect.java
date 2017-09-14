/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.module.conn;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.core.bean.HostBean;
import com.czp.ulc.core.message.MessageCenter;
import com.czp.ulc.core.message.MessageListener;
import com.czp.ulc.util.Utils;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Function:SSH链接
 *
 * @date:2017年3月27日/下午2:23:36
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public abstract class AbstractConnect implements Runnable, MessageListener<HostBean> {

	protected static Logger LOG = LoggerFactory.getLogger(AbstractConnect.class);

	protected MessageCenter messageCenter;

	protected int maxRetryTimes = 50;

	private int retryTimes = 0;

	protected Session session;

	protected HostBean server;

	protected boolean userStop;

	public AbstractConnect(HostBean server, MessageCenter messageCenter) {
		this.server = server;
		this.messageCenter = messageCenter;
		this.messageCenter.addConcumer(this);
	}

	/**
	 * 打开指定的通道
	 * 
	 * @param type
	 * @return
	 */
	protected Channel openChannel(String type) {
		try {
			if (session == null || !session.isConnected()) {
				LOG.info("start connect:{}", server);
				this.session = new JSch().getSession(server.getUser(), server.getHost(), server.getPort());
				this.session.setConfig("StrictHostKeyChecking", "no");
				this.session.setPassword(server.getPwd());
				this.session.connect(5000);
				this.retryTimes = 0;
				LOG.info("success connect:{}", server);
			}
			return session.openChannel(type);
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
	}

	/***
	 * 检测是否可以连接上
	 * 
	 * @return
	 * @throws JSchException
	 */
	public boolean canConnected() throws Exception {
		Channel ch = openChannel("exec");
		ch.connect();
		ch.disconnect();
		return true;
	}

	/***
	 * 执行命令
	 * 
	 * @param out
	 * @param cmd
	 * @throws IOException
	 */
	protected void sendCommad(OutputStream out, String cmd) throws IOException {
		out.write(cmd.getBytes());
		out.write('\n');
		out.flush();
	}

	/***
	 * 网络断开后自动重连
	 */
	protected void doRetryConnect() {
		if (!userStop && retryTimes++ < maxRetryTimes) {
			LOG.info("retry connect:{}, times:{}", server, retryTimes);
			Utils.sleep(3000);
			run();
		}
	}

	@Override
	public Class<HostBean> processClass() {
		return HostBean.class;
	}

	@Override
	public boolean onMessage(HostBean message, Map<String, Object> ext) {
		if (message.getId().equals(server.getId())) {
			String type = String.valueOf(ext.get("type"));
			if ("update".equals(type)) {
				// 直接断开,稍后会自动重连
				disconnection(false);
			} else if ("delete".equals(type)) {
				disconnection(true);
			}
		}
		return false;
	}

	@Override
	public void onExit() {
		disconnection(true);
	}

	/***
	 * 断开连接
	 */
	public void disconnection(boolean userStop) {
		this.userStop = userStop;
		this.session.disconnect();
	}

	public HostBean getServer() {
		return server;
	}

}
