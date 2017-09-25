package com.czp.ulc.core.zk;

import java.nio.ByteBuffer;
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

import com.czp.ulc.util.Utils;

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
	public static final String NODE_PATH = "/node/";

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
		registCurrentHostToNode();
	}

	public ZkClient getZkClient() {
		return zkClient;
	}

	/***
	 * 把自己注册到zk
	 */
	private void registCurrentHostToNode() {
		String host = Utils.innerInetIp();
		long time = System.currentTimeMillis();
		String nodePath = buildZkPath(NODE_PATH, host);
		byte[] data = ByteBuffer.allocate(8).putLong(time).array();
		zkClient.createEphemeralSequential(nodePath, data);
		LOG.info("success  to node path: {}", nodePath);
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
		int i = 0, len = path.length, size = len - 1;
		while (i < size) {
			String item = path[i++];
			sb.append(item);
			if (!item.endsWith("/"))
				sb.append("/");
		}
		sb.append(path[i]);
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
		if (zkClient != null)
			zkClient.close();
	}
}
