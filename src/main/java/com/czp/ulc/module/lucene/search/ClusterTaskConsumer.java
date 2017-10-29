package com.czp.ulc.module.lucene.search;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.core.ThreadPools;
import com.czp.ulc.module.lucene.DocField;
import com.czp.ulc.web.QueryCondtion;

/**
 * 集群模式下任务消费
 *
 * <li>创建人：coder_czp@126.com</li>
 * <li>创建时间：2017年10月29日</li>
 * 
 * @version 0.0.1
 */
public class ClusterTaskConsumer implements Runnable {

	private String topic;

	private String mqSerAddr;

	private LocalIndexSearcher searcher;

	private static final Logger LOG = LoggerFactory.getLogger(ClusterTaskConsumer.class);

	public ClusterTaskConsumer(String mqSerAddr) {
		this.mqSerAddr = mqSerAddr;
		ThreadPools.getInstance().run("Mapreduce-wait", this, true);
	}

	@Override
	public void run() {
		try {
			ZMQ.Context context = ZMQ.context(1);
			ZMQ.Socket sub = context.socket(ZMQ.SUB);
			sub.connect("tcp://" + mqSerAddr);
			sub.subscribe(topic.getBytes());

			LOG.info("subscribe:{}-{}", mqSerAddr, topic);
			while (!Thread.interrupted()) {
				try {
					byte[] topic = sub.recv(0);
					byte[] data = sub.recv(0);
					String dataStr = new String(data);
					String topicStr = new String(topic);
					LOG.debug("recv:{}-{}", topicStr, dataStr);

					doIndexSearch(dataStr);
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
	 * taskJson:{"server":"ip:port","reqId":100x,"query"：{...}}
	 * 
	 * @param taskJson
	 */
	private void doIndexSearch(String taskJson) {
		try {
			JSONObject task = JSONObject.parseObject(taskJson);
			// 哪个主机发起的查询,结果写回哪个主机
			String server = task.getString("server");
			// 查询ID,发起查询的主机用于区分不同的查询结果
			long reqId = task.getLongValue("reqId");
			// 查询条件
			JSONObject query = task.getJSONObject("query");
			QueryCondtion cdt = query.toJavaObject(QueryCondtion.class);
			CountDownLatch lock = new CountDownLatch(1);
			cdt.setAnalyzer(searcher.getAnalyzer());

			String[] filelds = cdt.isLoadLine() ? DocField.ALL_FEILD : DocField.NO_LINE_FEILD;
			searcher.search(new SearchCallback(cdt, filelds) {

				long now = System.currentTimeMillis();

				@Override
				public boolean handle(String host, String file, String line) {
					return true;
				}

				@Override
				public void onFinish(long allDoc, long allMatch) {
					long end = System.currentTimeMillis();
					long time = end - now;
					lock.countDown();
				}
			});
			lock.await(10, TimeUnit.MINUTES);
		} catch (Exception e) {
			LOG.error("query index error", e);
		}
	}

}
