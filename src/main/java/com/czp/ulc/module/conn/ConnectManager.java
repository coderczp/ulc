package com.czp.ulc.module.conn;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.core.ThreadPools;
import com.czp.ulc.core.bean.HostBean;
import com.czp.ulc.core.dao.MonitoConfigDao;
import com.czp.ulc.core.message.MessageCenter;
import com.czp.ulc.core.message.MessageListener;
import com.czp.ulc.util.Utils;
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

	private MessageCenter mqCenter;
	private MonitoConfigDao cfgDao;

	private JSch jSch = new JSch();
	private volatile boolean shutdown = false;
	private List<String> notFound = new ArrayList<String>();
	private Map<String, Session> maps = new ConcurrentHashMap<>();

	private static Logger LOG = LoggerFactory.getLogger(ConnectManager.class);

	public ConnectManager(MessageCenter mqCenter, MonitoConfigDao cfgDao) {
		notFound.add("host not found");
		this.cfgDao = cfgDao;
		this.mqCenter = mqCenter;
		mqCenter.addConcumer(this);
	}

	public Map<String, List<String>> exeInAll(String cmd) {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		for (Entry<String, Session> entry : maps.entrySet()) {
			List<String> res = exe(entry.getKey(), cmd);
			map.put(entry.getKey(), res);
		}
		return map;
	}

	public boolean exist(String host) {
		return maps.containsKey(host);
	}

	public List<String> exe(String hostName, String cmd) {
		Session session = maps.get(hostName);
		if (session == null)
			return notFound;
		return doExe(hostName, cmd, session);
	}

	public List<String> doExe(String hostName, String cmd, Session session) {
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
			LOG.info("sucess to execute:{} in host:{}", cmd, hostName);
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
		Session session = maps.get(bean.getName());
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
			maps.put(bean.getName(), session);
			
			if (bean.getStatus() == HostBean.STATUS_MONITOR) {
				startMonitor(bean);
			}
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 开启监控线程
	 * 
	 * @param bean
	 */
	private void startMonitor(HostBean bean) {
		RemoteLogCollector task = new RemoteLogCollector(bean, cfgDao, this, mqCenter);
		ThreadPools.getInstance().run("monitor-" + bean.getName(), task, true);
	}

	public Session getSession(HostBean bean) {
		Session session = maps.get(bean.getName());
		if (session != null && !session.isConnected()) {
			buildAndCacheSession(bean);
			session = maps.get(bean.getName());
		}
		return session;
	}

	/***
	 * 打开channel
	 * 
	 * @param hostId
	 * @param type
	 * @return
	 */
	public Channel openChannel(HostBean server, String type) {
		try {
			Session session = getSession(server);
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
		for (Entry<String, Session> item : maps.entrySet()) {
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
}
