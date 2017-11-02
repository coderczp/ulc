package com.czp.ulc.module.mapreduce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import com.czp.ulc.main.Application;
import com.czp.ulc.module.lucene.search.ILocalSearchCallback;
import com.czp.ulc.module.lucene.search.LocalIndexSearcher;
import com.czp.ulc.module.lucene.search.SearchResult;
import com.czp.ulc.module.lucene.search.SearchTask;
import com.czp.ulc.module.mapreduce.operation.IOperation;
import com.czp.ulc.module.mapreduce.rpc.RpcClientProxy;

/**
 * 集群模式下任务消费
 *
 * <li>创建人：coder_czp@126.com</li>
 * <li>创建时间：2017年10月29日</li>
 * 
 * @version 0.0.1
 */
public class SearchTaskConsumer implements Runnable {

	private String topic;

	private String mqSerAddr;

	private RpcClientProxy client;

	private MapreduceModule mpr;

	private static final Logger LOG = LoggerFactory.getLogger(SearchTaskConsumer.class);

	public SearchTaskConsumer(String topic, String mqSerAddr, RpcClientProxy client, MapreduceModule mpr) {
		this.mpr = mpr;
		this.topic = topic;
		this.client = client;
		this.mqSerAddr = mqSerAddr;
	}

	@Override
	public void run() {
		try {
			ZMQ.Context context = ZMQ.context(1);
			ZMQ.Socket sub = context.socket(ZMQ.SUB);
			sub.connect(mqSerAddr);
			sub.subscribe(topic.getBytes());

			LOG.info("subscribe:{}-{}", mqSerAddr, topic);
			while (!Thread.interrupted()) {
				try {
					byte[] topic = sub.recv(0);
					byte[] data = sub.recv(0);
					String topicStr = new String(topic);
					LOG.debug("recv topic {}", topicStr);
					doIndexSearch(data);
				} catch (Exception e) {
					LOG.error("mapreduce error", e);
				}
			}
			sub.close();
			context.term();
		} catch (Exception e) {
			LOG.error("create zmq socket err", e);
		}
	}

	/***
	 * taskJson:{"server":"rmi://ip:port/rpc","reqId":100x,"query"：{...}}
	 * 
	 * @param taskJson
	 */
	private void doIndexSearch(byte[] data) {
		try {
			MapReduceTask task = MapreduceModule.decodeTask(data);
			String type = task.getQuery().getType();
			String rpcUrl = task.getRpcUrl();
			
			if (rpcUrl.equals(mpr.getRpcUrl())) {
				LOG.info("this search request is send by myself");
				return;
			}
			
			IRemoteSearchCallback redSer = client.getServer(rpcUrl, IRemoteSearchCallback.class);
			IOperation op = mpr.getRegistTable().find(type);
			if (op == null) {
				LOG.info("unsupport operation:{}", type);
				return;
			}
			op.handle(redSer, getSearcher(),task);
		} catch (Exception e) {
			LOG.error("query index error", e);
		}
	}

	/** 不能用注入的方式,因为该类init是lucenmodule还没有加载 */
	private LocalIndexSearcher getSearcher() {
		return Application.getBean(LocalIndexSearcher.class);
	}
}
