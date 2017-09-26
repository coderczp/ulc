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
import com.czp.ulc.core.dao.HostDao;
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

	protected HostDao hostDao;
	protected MessageCenter mqCenter;
	protected MonitoConfigDao cfgDao;

	protected JSch jSch = new JSch();
	protected volatile boolean shutdown = false;
	protected List<String> notFound = new ArrayList<String>();
	protected Map<String, Session> maps = new ConcurrentHashMap<>();

	/** 不需要自动重练的链接 */
	protected Map<String, Boolean> notReConn = new ConcurrentHashMap<>();

	private static Logger LOG = LoggerFactory.getLogger(ConnectManager.class);

	public ConnectManager() {
		notFound.add("host not found");
	}

	public void setHostDao(HostDao hostDao) {
		this.hostDao = hostDao;
	}

	public void setMqCenter(MessageCenter mqCenter) {
		this.mqCenter = mqCenter;
	}

	public void setCfgDao(MonitoConfigDao cfgDao) {
		this.cfgDao = cfgDao;
	}

	public boolean isNotReConn(String hostName) {
		return notReConn.containsKey(hostName);
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
			LOG.info("host:{} has connected", bean.getHost());
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
			notReConn.remove(bean.getName());

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
		Session session = getSession(server);
		if (session != null) {
			notReConn.put(server.getName(), true);
			session.disconnect();
			LOG.info("disconnect  {}", server);
		}
	}

	@Override
	public boolean onMessage(HostBean message, Map<String, Object> ext) {
		if (!maps.containsKey(message.getName()))
			return false;

		Integer id = message.getId();
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
		notReConn.clear();
	}

	public boolean isShutdown() {
		return shutdown;
	}

	/***
	 * 异步链接所有被监控的机器
	 */
	public void onStart() {
		ThreadPools.getInstance().run("conn-moudle-start", () -> {
			Map<String, Object> param = new HashMap<>();
			param.put("status", HostBean.STATUS_MONITOR);
			for (HostBean host : hostDao.list(param)) {
				try {
					connect(host);
				} catch (Exception e) {
					LOG.info("connect err:" + host, e);
				}
			}
		}, true);
	}

	@Override
	public Class<HostBean> processClass() {
		return HostBean.class;
	}
}
