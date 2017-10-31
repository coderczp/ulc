package com.czp.ulc.module.mapreduce;

import java.net.URI;
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
import com.czp.ulc.module.mapreduce.rpc.RpcClient;
import com.czp.ulc.module.mapreduce.rpc.TransportImpl;

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

	private RpcClient rpcClient;
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

			rpcClient = new RpcClient();

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

	/***
	 * 等待mp任务
	 * 
	 * @param mqPubAddr
	 * @param mqPubTopic
	 * @param rpcClient
	 */
	private void startZMQSUB(String mqPubAddr, RpcClient rpcClient, String mqPubTopic) {
		SearchTaskConsumer task = new SearchTaskConsumer(mqPubTopic, mqPubAddr, rpcClient, this);
		ThreadPools.getInstance().run("ZMQ-sub", task, true);
	}

	// rpcAddr:like rmi://127.0.0.1:8333/rpc
	private void startRPCServer(String rpcAddr) throws Exception {
		URI uri = new URI(rpcAddr);
		rpcUrl = rpcAddr;
		namingCtx = new InitialContext();
		TransportImpl transport = new TransportImpl();

		LocateRegistry.createRegistry(uri.getPort());
		namingCtx.rebind(rpcUrl, transport);
		LOG.info("rpc server listen at:{}", rpcUrl);

		exportRPCServer(transport);
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
		try {
			MapReduceTask mTask = new MapReduceTask();
			mTask.setReqId(System.nanoTime());
			mTask.setQuery(task.getQuery());
			mTask.setRpcUrl(rpcUrl);
			boolean res = zmqReq.send(encodeMapReduceTask(mTask));
			tasks.put(mTask.getReqId(), task);
			LOG.info("send remote search return:{}", res);
		} catch (Exception e) {
			LOG.error("send remote search err", e);
		}
	}

	public void cleanTask(long reqId) {
		tasks.remove(reqId);
	}

	public SearchTask getMaprequceTask(long reqId) {
		return tasks.get(reqId);
	}

	public static byte[] encodeMapReduceTask(MapReduceTask task) {
		return JSONObject.toJSONString(task).getBytes(Charset.forName("utf-8"));
	}

	public static MapReduceTask decodeMapReduceTask(byte[] data) {
		String json = new String(data, Charset.forName("utf-8"));
		return JSONObject.parseObject(json, MapReduceTask.class);
	}

	@Override
	public boolean stop() {
		try {
			zmqReq.close();
			context.term();
			rpcClient.stop();
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
