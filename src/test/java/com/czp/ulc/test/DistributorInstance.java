package com.czp.ulc.test;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.core.bean.HostBean;
import com.czp.ulc.core.dao.HostDao;
import com.czp.ulc.core.dao.MonitoConfigDao;

/**
 * 分布式环境下的实例：<br>
 * <li>1.zk注册自己</li>
 * <li>2.查询没有被链接的主机</li>
 * <li>3.创建建立连接的主机节点,并监听</li>
 * 
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月18日 上午10:17:15</li>
 * 
 * @version 0.0.1
 */

public class DistributorInstance implements Watcher, StatCallback {

	private ZooKeeper zk;
	private HostDao hostDao;
	private MonitoConfigDao mDao;
	private String rootNode = "ulc";
	private String appNode = String.format("%s/apps", rootNode);
	private static Logger LOG = LoggerFactory.getLogger(DistributorInstance.class);

	public DistributorInstance(String zkServer, HostDao hostDao, MonitoConfigDao mDao) {
		this.hostDao = hostDao;
		this.mDao = mDao;

		try {
			this.zk = new ZooKeeper(zkServer, 5000, this);
			createIfNotExist(rootNode, "ulc".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void createIfNotExist(String node, byte[] data) throws Exception {
		if (zk.exists(node, false) == null) {
			zk.create(node, data, null, CreateMode.PERSISTENT);
		}
	}

	@Override
	public void processResult(int rc, String path, Object ctx, Stat stat) {

	}

	@Override
	public void process(WatchedEvent event) {

	}

	public void run() {
		try {
			registCurrentInstance();
			List<String> hosts = getHasMonitorHost();
			connectNotMonitorHost(hosts);
		} catch (Exception e) {
			LOG.error("error", e);
		}
	}

	/***
	 * 监控未监控的主机
	 * 
	 * @param hosts
	 * @throws Exception
	 */
	private void connectNotMonitorHost(List<String> hosts) throws Exception {
		Map<String, Object> param = new HashMap<>();
		param.put("excludeNames", hosts);
		byte[] data = getUUID().getBytes();
		List<HostBean> list = hostDao.list(param);
		for (HostBean host : list) {
			String path = String.format("%s/%s", host.getName());
			if (zk.exists(path, false) == null) {
				zk.create(path, data, null, CreateMode.EPHEMERAL);
				LOG.info("monitor:{}", host);
			}
		}
	}

	/***
	 * 获取zk上已经监控的主机名
	 * 
	 * <pre>
	 * ---/root <br>
	 * -----/apps<br>
	 * -------/app0<br>
	 * -----------monitor_host0<br>
	 * -----------monitor_host1<br>
	 * * -----/app1<br>
	 * -----------monitor_host0<br>
	 * -----------monitor_host1<br>
	 * 
	 * <pre>
	 * 
	 * @return
	 * @throws Exception
	 */
	private List<String> getHasMonitorHost() throws Exception {
		List<String> apps = this.zk.getChildren(appNode, this);
		if (apps.isEmpty()) {
			return new ArrayList<>();
		}

		return apps;
	}

	/***
	 * 注册自己到zk
	 * 
	 * @throws Exception
	 */
	private void registCurrentInstance() throws Exception {
		String uuid = getUUID();
		byte[] data = System.getProperty("user.dir").getBytes();
		String path = String.format("%s/%s", appNode, uuid);
		this.zk.create(path, data, null, CreateMode.EPHEMERAL);
	}

	private String getUUID() {
		String pid = getCurrentPid();
		String hostIp = getCurrentHostIp();
		return String.format("%s_%s", hostIp, pid);
	}

	private String getCurrentPid() {
		String name = ManagementFactory.getRuntimeMXBean().getName();
		String pid = name.split("@")[0];
		return pid;
	}

	private String getCurrentHostIp() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
