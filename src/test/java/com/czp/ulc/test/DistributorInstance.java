package com.czp.ulc.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.collect.ConnectManager;
import com.czp.ulc.common.bean.HostBean;
import com.czp.ulc.common.dao.HostDao;
import com.czp.ulc.common.dao.MonitoFileDao;
import com.czp.ulc.common.util.Utils;

/**
 * 分布式环境下的实例：<br>
 * <li>1.zk注册自己</li> <li>2.查询没有被链接的主机</li> <li>3.创建建立连接的主机节点,并监听</li>
 * 
 * <li>创建人：Jeff.cao</li> <li>创建时间：2017年4月18日 上午10:17:15</li>
 * 
 * @version 0.0.1
 */

public class DistributorInstance implements Watcher, StatCallback {

	private ZooKeeper zk;
	private HostDao hostDao;
	private MonitoFileDao mDao;
	private String rootNode = "ulc";
	private String appNode = String.format("%s/apps", rootNode);
	private static Logger LOG = LoggerFactory.getLogger(DistributorInstance.class);

	public DistributorInstance(String zkServer, HostDao hostDao, MonitoFileDao mDao) {
		try {
			this.mDao = mDao;
			this.hostDao = hostDao;
			this.zk = new ZooKeeper(zkServer, 5000, this);
			this.createIfNotExist(rootNode, "ulc".getBytes());
			this.createIfNotExist(appNode, "apps".getBytes());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void processResult(int rc, String path, Object ctx, Stat stat) {

	}

	@Override
	public void process(WatchedEvent event) {
		String path = event.getPath();
		// 如果是app节点变化,意味着有其他app节点加入或退出,要重新做负载均衡
		if (event.getType() == EventType.NodeChildrenChanged && path.equals(appNode)) {
            
		}
	}

	public void run() {
		try {
			registCurrentInstance();
			List<String> hosts = getHasMonitorHost();
			connectHost(hosts);
			watchNode(appNode);
		} catch (Exception e) {
			LOG.error("error", e);
		}
	}

	/***
	 * 监控指定的节点
	 * 
	 * @param path
	 */
	private void watchNode(String path) {
		try {
			zk.exists(path, this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/***
	 * 监控未监控的主机
	 * 
	 * @param hosts
	 * @throws Exception
	 */
	private void connectHost(List<String> hosts) throws Exception {
		Map<String, Object> param = new HashMap<>();
		param.put("excludeNames", hosts);
		byte[] data = getUUID().getBytes();
		List<HostBean> list = hostDao.list(param);
		for (HostBean host : list) {
			String path = String.format("%s/%s", host.getName());
			createIfNotExist(path, data);
			ConnectManager.getInstance().connectAndMonitor(host, mDao);
		}
	}

	/***
	 * 获取zk上已经监控的主机名
	 * 
	 * ---/root <br>
	 * -----/apps<br>
	 * -------/app0<br>
	 * -----------monitor_host0<br>
	 * -----------monitor_host1<br>
	 * --------/app1<br>
	 * -----------monitor_host3<br>
	 * -----------monitor_host4<br>
	 * 
	 * @return
	 * @throws Exception
	 */
	private List<String> getHasMonitorHost() throws Exception {
		List<String> apps = this.zk.getChildren(appNode, this);
		if (apps.isEmpty()) {
			return Collections.emptyList();
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
		String pid = Utils.getProcessId();
		String hostIp = Utils.getCurrentHostIp();
		return String.format("%s_%s", hostIp, pid);
	}

	private void createIfNotExist(String node, byte[] data) throws Exception {
		if (zk.exists(node, false) == null) {
			zk.create(node, data, null, CreateMode.PERSISTENT);
			LOG.info("success to create node:{} in zk", node);
		}
	}

}
