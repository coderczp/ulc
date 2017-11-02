package com.czp.ulc.module.mapreduce;

import java.nio.charset.Charset;
import java.rmi.registry.LocateRegistry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.zeromq.ZMQ;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.core.ThreadPools;
import com.czp.ulc.module.IModule;
import com.czp.ulc.module.lucene.search.SearchTask;
import com.czp.ulc.module.mapreduce.operation.OptionRegistTable;
import com.czp.ulc.module.mapreduce.rpc.RpcClientProxy;
import com.czp.ulc.module.mapreduce.rpc.TransportImpl;
import com.czp.ulc.util.Utils;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月30日</li>
 * 
 * @version 0.0.1
 */
@Service
public class MapreduceModule implements IModule {

	private String rpcUrl;

	@Autowired
	private Environment env;

	private Context namingCtx;

	private ZMQ.Socket zmqReq;

	private ZMQ.Context context;

	private RpcClientProxy rpcClient;

	private OptionRegistTable registTable = new OptionRegistTable();

	/***
	 * 保存Mapreduce的ID,其他进程搜索返回时回写结果给当前进程
	 */
	private ConcurrentHashMap<Long, SearchTask> tasks = new ConcurrentHashMap<>();

	private static final Logger LOG = LoggerFactory.getLogger(MapreduceModule.class);

	@Override
	public boolean start(SingletonBeanRegistry ctx) {
		try {
			String mqPubTopic = env.getProperty("mq.pub.topic");
			String rpcAddr = env.getProperty("rpc.server.address");
			String mqPullAddr = env.getProperty("mq.pull.server.address");
			String mqPubAddr = env.getProperty("mq.pub.server.address");

			Objects.requireNonNull(mqPubTopic, "mq.pub.topic");
			Objects.requireNonNull(rpcAddr, "rpc.server.address");
			Objects.requireNonNull(mqPullAddr, "mq.rep.server.address");
			Objects.requireNonNull(mqPubAddr, "mq.pub.server.address");

			rpcClient = new RpcClientProxy();

			startRPCServer(rpcAddr);
			startZMQPull(mqPullAddr);
			startZMQSUB(mqPubAddr, rpcClient, mqPubTopic);
		} catch (Exception e) {
			throw new RuntimeException("fail to start rpc server", e);
		}
		return true;
	}

	public String getRpcUrl() {
		return rpcUrl;
	}

	public OptionRegistTable getRegistTable() {
		return registTable;
	}

	/***
	 * 等待mp任务
	 * 
	 * @param mqPubAddr
	 * @param mqPubTopic
	 * @param rpcClient
	 */
	private void startZMQSUB(String mqPubAddr, RpcClientProxy rpcClient, String mqPubTopic) {
		SearchTaskConsumer task = new SearchTaskConsumer(mqPubTopic, mqPubAddr, rpcClient, this);
		ThreadPools.getInstance().run("ZMQ-sub", task, true);
	}

	// rpcAddr:like rmi://127.0.0.1:8333/rpc
	private void startRPCServer(String addr) throws Exception {
		int ipStart = addr.indexOf("//");
		int ipEnd = addr.indexOf(':', ipStart);
		int portEnd = addr.indexOf('/', ipEnd);
		String ip = addr.substring(ipStart + 2, ipEnd);
		int port = Integer.parseInt(addr.substring(ipEnd + 1, portEnd));

		if (!"*".equals(ip)) {
			System.setProperty("java.rmi.server.hostname", ip);
		} else {
			// 如果指定bind*,则取内网ip
			addr = addr.replaceAll("\\*", Utils.innerInetIp());
		}
		TransportImpl transport = new TransportImpl();
		namingCtx = new InitialContext();
		LocateRegistry.createRegistry(port);
		namingCtx.rebind(addr, transport);
		exportRPCServer(transport);
		rpcUrl = addr;

		LOG.info("rpc server listen at:{}", rpcUrl);
	}

	// 导出需要远程调用的服务
	private void exportRPCServer(TransportImpl transport) {
		IRemoteSearchCallback rSear = new RemoteSearchCallbackImpl(this);
		transport.export(IRemoteSearchCallback.class.getName(), rSear);
	}

	private void startZMQPull(String mqReqAddr) {
		context = ZMQ.context(1);
		zmqReq = context.socket(ZMQ.PUSH);
		zmqReq.connect(mqReqAddr);
		// 这两个参数很重要,设置为0表示发送失败立即返回
		zmqReq.setLinger(0);
		zmqReq.setReceiveTimeOut(0);
	}

	/**
	 * 请求mapreduce
	 * 
	 * @param task
	 */
	public void doRemoteSearch(SearchTask task) {
		Objects.requireNonNull(task.getQuery().getType(), "query type is null");
		MapReduceTask mTask = new MapReduceTask();
		mTask.setReqId(System.nanoTime());
		mTask.setQuery(task.getQuery());
		mTask.setRpcUrl(rpcUrl);
		try {
			byte[] bytes = encodeTask(mTask);
			boolean res = zmqReq.send(bytes);
			tasks.put(mTask.getReqId(), task);
			LOG.info("send remote search return:{}", res);
		} catch (Exception e) {
			LOG.error("send remote search err", e);
		}
	}

	public SearchTask cleanTask(long reqId) {
		return tasks.remove(reqId);
	}

	public SearchTask getMaprequceTask(long reqId) {
		return tasks.get(reqId);
	}

	public static byte[] encodeTask(MapReduceTask task) {
		return JSONObject.toJSONString(task).getBytes(Charset.forName("utf-8"));
	}

	public static MapReduceTask decodeTask(byte[] data) {
		String json = new String(data, Charset.forName("utf-8"));
		return JSONObject.parseObject(json, MapReduceTask.class);
	}

	@Override
	public boolean stop() {
		try {
			zmqReq.close();
			context.term();
			namingCtx.close();
		} catch (NamingException e) {
			LOG.error("rpc close err", e);
		}
		return true;
	}

	@Override
	public String name() {
		return "Mapreduce module";
	}

	@Override
	public int order() {
		return 0;
	}

}
