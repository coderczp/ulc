package com.czp.ulc.collect;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.common.MessageCenter;
import com.czp.ulc.common.MessageListener;
import com.czp.ulc.common.bean.HostBean;
import com.czp.ulc.common.dao.MonitoFileDao;
import com.czp.ulc.common.util.Utils;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * @dec Function
 * @author coder_czp@126.com
 * @date 2017年4月2日/下午12:38:59
 * @copyright coder_czp@126.com
 *
 */
public class ConnectManager implements MessageListener<HostBean> {

	private JSch jSch = new JSch();
	private volatile boolean shutdown = false;
	private List<String> notFound = new ArrayList<String>();
	private Map<Integer, Session> maps = new ConcurrentHashMap<>();

	private static ConnectManager INSTANCE = new ConnectManager();
	private static Logger LOG = LoggerFactory.getLogger(ConnectManager.class);

	private ConnectManager() {
		notFound.add("host not found");
		MessageCenter.getInstance().addConcumer(this);
	}

	public static ConnectManager getInstance() {
		return INSTANCE;
	}

	public List<String> exe(Integer hostId, String cmd) {
		Session session = maps.get(hostId);
		if (session == null)
			return notFound;

		List<String> res = new LinkedList<>();
		try {
			ByteArrayOutputStream err = new ByteArrayOutputStream();
			ChannelExec channel = (ChannelExec) session.openChannel("exec");
			channel.setCommand(cmd);
			channel.setInputStream(null);
			channel.setErrStream(err);
			channel.connect();
			res = Utils.readLines(channel.getInputStream());
			String errorInfo = err.toString();
			if (!errorInfo.isEmpty()) {
				res.add(errorInfo);
			}
			channel.disconnect();
			LOG.info("sucess to execute:{} in host:{}", cmd, hostId);
		} catch (Exception e) {
			LOG.error("execute error:" + cmd, e);
			res.add("server error,try again," + e);
		}
		return res;
	}

	/***
	 * 添加主机并建立连接
	 * 
	 * @param bean
	 */
	public synchronized void connect(HostBean bean) {
		Session session = maps.get(bean.getId());
		if (session != null && session.isConnected()) {
			LOG.info("host:{} has connected");
			return;
		}
		buildAndCacheSession(bean);
	}

	private void buildAndCacheSession(HostBean bean) {
		try {
			LOG.info("start connect:{}", bean);
			Session session = jSch.getSession(bean.getUser(), bean.getHost(), bean.getPort());
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(Utils.decrypt(bean.getPwd()));
			session.connect(5000);
			LOG.info("success connect:{}", bean);
			maps.put(bean.getId(), session);
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
	}

	/***
	 * 打开channel
	 * 
	 * @param hostId
	 * @param type
	 * @return
	 */
	public Channel openChannel(HostBean server, String type) {
		Session session = maps.get(server.getId());
		try {
			if (!session.isConnected()) {
				buildAndCacheSession(server);
				session = maps.get(server.getId());
			}
			return session == null ? null : session.openChannel(type);
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
	}

	/***
	 * 关闭连接
	 * 
	 * @param server
	 */
	public synchronized void disconnect(HostBean server) {
		Session session = maps.get(server.getId());
		if (session != null) {
			session.disconnect();
		}
	}

	@Override
	public boolean onMessage(HostBean message, Map<String, Object> ext) {
		Integer id = message.getId();
		if (!maps.containsKey(id))
			return false;

		String type = String.valueOf(ext.get("type"));
		if ("update".equals(type)) {
			disconnect(message);
		} else if ("delete".equals(type)) {
			Session sesssion = maps.remove(id);
			if (sesssion != null)
				sesssion.disconnect();
		}
		return true;
	}

	@Override
	public void onExit() {
		shutdown = true;
		for (Entry<Integer, Session> item : maps.entrySet()) {
			maps.remove(item.getKey());
			item.getValue().disconnect();
		}
	}

	public boolean isShutdown() {
		return shutdown;
	}

	@Override
	public Class<HostBean> processClass() {
		return HostBean.class;
	}

	public void connectAndMonitor(HostBean host, MonitoFileDao mDao) {
		connect(host);
		RemoteLogCollector.monitorIfNotExist(host, mDao);
	}
}
