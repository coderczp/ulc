package com.czp.ulc.module.conn;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.czp.ulc.core.ThreadPools;
import com.czp.ulc.core.bean.HostBean;
import com.czp.ulc.core.zk.ZkListener;
import com.czp.ulc.core.zk.ZkManager;
import com.czp.ulc.util.Utils;

/**
 * 集群模式的链接管理器
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月25日</li>
 * 
 * @version 0.0.1
 */

public class ClusterConnManager extends ConnectManager implements ZkListener {

	private String port;
	private ZkManager zkMgr;
	private ZkClient zkClient;
	private String currentPath;

	/** 当前node监控的机器 */
	private List<HostBean> monitorHosts;
	private static Logger LOG = LoggerFactory.getLogger(ClusterConnManager.class);
	private ExecutorService worker = ThreadPools.getInstance().newPool("zk-event-handler", 1);

	public ClusterConnManager(String port, ZkManager zkMgr) {
		super();
		this.port = port;
		this.zkMgr = zkMgr;
		this.zkClient = zkMgr.getZkClient();
	}

	@Override
	public void onStart() {
		zkMgr.addListenr(this);
		registCurrentHostToNode();
		doLoadBalance();
		zkClient.subscribeChildChanges(ZkManager.ROOT_PATH, this);
	}

	@Override
	public void handleDataChange(String dataPath, Object data) throws Exception {

	}

	@Override
	public void handleDataDeleted(String dataPath) throws Exception {

	}

	@Override
	public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
		worker.execute(() -> {
			try {
				List<HostBean> hasMonitor = monitorHosts;
				// 查找当前主机应该监控的机器
				List<HostBean> hosts = getCurrentNodeMonitorHosts(currentChilds);
				// 跟当前主机已经监控的节点做比较,如果不属于自己监控的节点则断链
				Iterator<HostBean> waitDel = hasMonitor.iterator();
				while (waitDel.hasNext()) {
					HostBean host = waitDel.next();
					if (!hosts.contains(host)) {
						disconnect(host);
						waitDel.remove();
					}
				}
				// 属于自己监控但未监控的则添加
				Iterator<HostBean> waitAdd = hosts.iterator();
				while (waitAdd.hasNext()) {
					HostBean host = waitAdd.next();
					if (!hasMonitor.contains(host)) {
						connect(host);
					}
				}
			} catch (Exception e) {
				LOG.error("process zk event error", e);
			}
		});
	}

	/***
	 * 把自己注册到zk
	 */
	private void registCurrentHostToNode() {
		String host = Utils.innerInetIp();
		String path = String.format("%s:%s", host, port);
		currentPath = ZkManager.buildnNodePath(path);
		long time = System.currentTimeMillis();
		byte[] data = ByteBuffer.allocate(8).putLong(time).array();
		zkClient.createEphemeral(currentPath, data);
		LOG.info("success  to node path: {}", currentPath);
	}

	/***
	 * 根据当前node的数量和被监控机器的数量做负载均衡<br>
	 * 如: node-[node1,node2,node3]<br>
	 * host->[h1,h2,h2,h4,h5,h6]<br>
	 * 则每个node监控两个机器,node是有编号的<br>
	 * node1负责[h1,h2]每个机器只管链接当前的host
	 */
	private void doLoadBalance() {
		List<String> nodes = zkClient.getChildren(ZkManager.ROOT_PATH);
		if (nodes.size() == 1) {
			monitorHosts = queryAllHosts();
			asynConnect(monitorHosts);
			return;
		}
		doSharding(nodes);
	}

	private void doSharding(List<String> nodes) {
		monitorHosts = getCurrentNodeMonitorHosts(nodes);
		asynConnect(monitorHosts);
	}

	private List<HostBean> getCurrentNodeMonitorHosts(List<String> nodes) {
		int nodeOrder = getCurrentNodeOrder(nodes);
		List<HostBean> hosts = sharding(nodes.size()).get(nodeOrder);
		Assert.notEmpty(hosts, "current node shard result is empty");
		return hosts;
	}

	private List<HostBean> queryAllHosts() {
		Map<String, Object> param = new HashMap<>();
		param.put("status", HostBean.STATUS_MONITOR);
		return hostDao.list(param);
	}

	private void asynConnect(List<HostBean> hosts) {
		ThreadPools.getInstance().run("conn-moudle-start", () -> {
			for (HostBean host : hosts) {
				try {
					connect(host);
				} catch (Exception e) {
					LOG.info("connect err:" + host, e);
				}
			}
		}, true);
	}

	// 将host按ID排序再与node的编号取模,计算出每个node负载几个host
	private Map<Integer, List<HostBean>> sharding(int nodeSize) {
		Map<Integer, List<HostBean>> shard = new HashMap<>();
		List<HostBean> list = queryAllHosts();
		int hostSize = list.size();
		for (int i = 0; i < hostSize; i++) {
			HostBean host = list.get(i);
			int nodeIndex = i % nodeSize;
			if (shard.get(nodeIndex) == null) {
				shard.put(nodeIndex, new LinkedList<>());
			}
			shard.get(nodeIndex).add(host);
		}
		LOG.info("sharding reulst:{}", shard);
		return shard;
	}

	private int getCurrentNodeOrder(List<String> nodes) {
		int i = 0;
		for (String string : nodes) {
			if (currentPath.contains(string))
				return i;
			i++;
		}
		throw new RuntimeException("can't find zkNode for:" + currentPath);
	}

}
