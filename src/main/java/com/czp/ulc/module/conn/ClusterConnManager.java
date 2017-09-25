package com.czp.ulc.module.conn;

import java.util.List;

import org.I0Itec.zkclient.ZkClient;

import com.czp.ulc.core.zk.ZkListener;
import com.czp.ulc.core.zk.ZkManager;

/**
 * 集群模式的链接管理器
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月25日</li>
 * 
 * @version 0.0.1
 */

public class ClusterConnManager extends ConnectManager implements ZkListener {

	private ZkManager zkMgr;

	public void setZkMgr(ZkManager zkMgr) {
		this.zkMgr = zkMgr;
	}

	@Override
	public void onStart() {
		zkMgr.addListenr(this);
		doLoadBalance();
	}

	@Override
	public void handleDataChange(String dataPath, Object data) throws Exception {

	}

	@Override
	public void handleDataDeleted(String dataPath) throws Exception {

	}

	@Override
	public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {

	}

	/***
	 * 根据当前node的数量和被监控机器的数量做负载均衡<br>
	 * 如: node-[node1,node2,node3]<br>
	 * host->[h1,h2,h2,h4,h5,h6]<br>
	 * 则每个node监控两个机器,node是有编号的<br>
	 * node1负责[h1,h2]每个机器只管链接当前的host
	 */
	private void doLoadBalance() {
		ZkClient zkClient = zkMgr.getZkClient();
		List<String> nodes = zkClient.getChildren(ZkManager.NODE_PATH);
	}
}
