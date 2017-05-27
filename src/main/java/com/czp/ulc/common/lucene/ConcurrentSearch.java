/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.common.lucene;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.collect.handler.SearchCallback;
import com.czp.ulc.common.shutdown.ShutdownCallback;
import com.czp.ulc.common.shutdown.ShutdownManager;
import com.czp.ulc.common.util.Utils;

/**
 * Function:并行搜索
 *
 * @date:2017年5月27日/下午2:37:04
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class ConcurrentSearch implements ShutdownCallback {

	private ExecutorService worker;
	private ConcurrentHashMap<File, Long> modifyMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<File, IndexSearcher> openedDir = new ConcurrentHashMap<>();
	private static final Logger LOG = LoggerFactory.getLogger(ConcurrentSearch.class);

	public ConcurrentSearch(int threadSize) {
		this.worker = Executors.newFixedThreadPool(threadSize);
		ShutdownManager.getInstance().addCallback(this);
	}

	private void doSearch(AtomicBoolean isStop, SearchCallback callBack, IndexSearcher searcher, String host, long total, CountDownLatch lock) {
		try {
			Set<String> feilds = callBack.getFeilds();
			Query query = callBack.getQuery();
			TopDocs docs = searcher.search(query, callBack.getSize());
			for (ScoreDoc scoreDoc : docs.scoreDocs) {
				Document doc = searcher.doc(scoreDoc.doc, feilds);
				String file = doc.get(DocField.FILE);
				String line = doc.get(DocField.LINE);
				if (!callBack.handle(host, file, line, docs.totalHits, total)) {
					isStop.set(true);
					break;
				}
			}
			lock.countDown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSystemExit() {
		worker.shutdownNow();
	}

	public void search(SearchCallback search, ConcurrentHashMap<String, TreeMap<Long, File>> indexDirMap, long fileDocs) {
		AtomicBoolean isStop = new AtomicBoolean();
		Map<String, Collection<File>> dirs = findMatchIndexDir(search, indexDirMap);
		CountDownLatch lock = new CountDownLatch(dirs.size());
		for (Entry<String, Collection<File>> item : dirs.entrySet()) {
			if (isStop.get())
				return;

			for (File indexDir : item.getValue()) {
				if (isStop.get())
					return;

				String host = item.getKey();
				IndexSearcher reader = openedDir.get(indexDir);
				if (reader != null && modifyMap.get(indexDir) >= indexDir.lastModified()) {
					worker.execute(() -> doSearch(isStop, search, reader, host, fileDocs,lock));
				} else {
					worker.execute(() -> {
						IndexSearcher searcher = getCachedSearch(indexDir, host);
						doSearch(isStop, search, searcher, host, fileDocs,lock);
					});
				}
			}
		}
		try {
			lock.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private IndexSearcher getCachedSearch(File file, String host) {
		try {
			IndexSearcher reader = openedDir.get(file);
			if (reader != null && modifyMap.get(file) >= file.lastModified()) {
				return reader;
			}
			// 锁住目录
			synchronized (file) {
				// 关闭过期目录
				if (reader != null) {
					openedDir.remove(file).getIndexReader().close();
				}
				if (!openedDir.containsKey(file)) {
					LOG.info("start open index:{}", file);
					FSDirectory open = FSDirectory.open(file.toPath());
					DirectoryReader newReader = DirectoryReader.open(open);
					openedDir.put(file, new IndexSearcher(newReader));
					modifyMap.put(file, file.lastModified());
					LOG.info("sucess to open index:{}", file);
				}
				return openedDir.get(file);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 找到匹配时间点的索引目录
	 * 
	 * @param hosts
	 * @param start
	 * @param end
	 * @return
	 */
	public Map<String, Collection<File>> findMatchIndexDir(SearchCallback searcher,
			ConcurrentHashMap<String, TreeMap<Long, File>> indexDirMap) {
		long from = Utils.igroeHMSTime(searcher.getBegin()).getTime();
		long to = Utils.igroeHMSTime(searcher.getEnd()).getTime();
		Map<String, Collection<File>> map = new HashMap<>();
		Set<String> hosts = searcher.getHosts();
		if (hosts != null && !hosts.isEmpty()) {
			for (String host : hosts) {
				TreeMap<Long, File> indexs = indexDirMap.get(host);
				if (indexs != null)
					map.put(host, indexs.subMap(from, true, to, true).values());
			}
		} else {
			for (Entry<String, TreeMap<Long, File>> entry : indexDirMap.entrySet()) {
				map.put(entry.getKey(), entry.getValue().values());
			}
		}
		return map;
	}

	public long count(SearchCallback search,ConcurrentHashMap<String, Object> json,ConcurrentHashMap<String, TreeMap<Long, File>> indexDirMap) {
		AtomicLong count = new AtomicLong();
		Map<String, Collection<File>> dirs = findMatchIndexDir(search, indexDirMap);
		CountDownLatch lock = new CountDownLatch(dirs.size());
		for (Entry<String, Collection<File>> item : dirs.entrySet()) {
			for (File indexDir : item.getValue()) {
				String host = item.getKey();
				IndexSearcher reader = openedDir.get(indexDir);
				if (reader != null && modifyMap.get(indexDir) >= indexDir.lastModified()) {
					try {
						int count2 = reader.count(search.getQuery());
						count.getAndAdd(count2);
						json.put(host, count2);
						lock.countDown();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					worker.execute(() -> {
						try {
							IndexSearcher searcher = getCachedSearch(indexDir, host);
							int count2 = searcher.count(search.getQuery());
							count.getAndAdd(count2);
							json.put(host, count2);
							lock.countDown();
						} catch (Exception e) {
							e.printStackTrace();
						}
					});
				}
			}
		}
		try {
			lock.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return count.get();
	}
}
