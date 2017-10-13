package com.czp.ulc.core.zk;

import java.util.Objects;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月21日</li>
 * 
 * @version 0.0.1
 */
@Service
public class ZkManager {

	@Autowired
	private Environment env;

	private ZkClient zkClient;

	/** 分片节点根路径 */
	public static final String ROOT_PATH = "/nodes";

	private Vector<ZkListener> listeners = new Vector<ZkListener>();

	private static final Logger LOG = LoggerFactory.getLogger(ZkManager.class);

	@PostConstruct
	public void start() {
		String connTimeout = env.getProperty("zk.conn.timeout", "20000");
		String zkServer = env.getProperty("zk.server.list");
		if (zkServer == null) {
			LOG.info("not found zk.server.list,use singleton model");
			return;
		}
		zkClient = new ZkClient(zkServer, Integer.valueOf(connTimeout));
		LOG.info("start cluster model zk:{}", zkServer);
		makeRootNodeIfRequired();
	}

	public ZkClient getZkClient() {
		return zkClient;
	}

	private void makeRootNodeIfRequired() {
		try {
			String rootNode = ROOT_PATH;
			if (!zkClient.exists(rootNode)) {
				zkClient.createPersistent(rootNode);
				LOG.info("success to create root node");
			}
		} catch (Throwable e) {
			LOG.error("fail to create root node,maybe exist {}", e.getMessage());
		}
	}

	/***
	 * 构建zk路径
	 * 
	 * @param root
	 * @param path
	 * @return
	 */
	public static String buildnNodePath(String... path) {
		Objects.requireNonNull(path, "path is required");
		StringBuilder sb = new StringBuilder(ROOT_PATH);
		for (String string : path) {
			sb.append("/").append(string);
		}
		return sb.toString();
	}

	/***
	 * 构建zk路径
	 * 
	 * @param root
	 * @param path
	 * @return
	 */
	public static String buildZkPath(String root, String... path) {
		Objects.requireNonNull(path, "path is required");
		StringBuilder sb = new StringBuilder(root);
		for (String string : path) {
			sb.append("/").append(string);
		}
		return sb.toString();
	}

	/***
	 * 是否是集群模式
	 * 
	 * @return
	 */
	public boolean isClusterModel() {
		return zkClient != null;
	}

	/***
	 * 添加监听器
	 * 
	 * @param listener
	 */
	public void addListenr(ZkListener listener) {
		listeners.add(listener);
	}

	@PreDestroy
	public void onStop() {
		if (zkClient != null) {
			zkClient.close();
		}
	}
}
