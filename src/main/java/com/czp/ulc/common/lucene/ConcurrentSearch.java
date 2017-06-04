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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.collect.handler.LogIndexHandler;
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

	private File indexDir;
	private ExecutorService worker;
	private ConcurrentHashMap<File, Long> modifyMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<File, IndexSearcher> openedDir = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, TreeMap<Long, File>> indexDirMap = new ConcurrentHashMap<>();

	private static final Logger LOG = LoggerFactory.getLogger(ConcurrentSearch.class);

	public ConcurrentSearch(int threadSize, File indexDir) {
		this.indexDir = indexDir;
		this.worker = Executors.newFixedThreadPool(threadSize);
		ShutdownManager.getInstance().addCallback(this);
	}

	public void loadAllIndexDir() {
		try {
			SimpleDateFormat sp = new SimpleDateFormat(LogIndexHandler.FORMAT);
			for (File file : indexDir.listFiles()) {
				String host = file.getName();
				TreeMap<Long, File> indexMap = indexDirMap.get(host);
				if (indexMap == null) {
					indexMap = new TreeMap<>();
					indexDirMap.put(host, indexMap);
				}
				// 第二层为时间目录 包含索引和数据目录
				for (File index : file.listFiles()) {
					if (!index.isDirectory())
						continue;
					modifyMap.put(index, index.lastModified());
					indexMap.put(sp.parse(index.getName()).getTime(), index);
				}
			}
			LOG.info("load all index");
		} catch (ParseException e) {
			LOG.error("ParseException error", e);
		}
	}

	private void ansySearch(AtomicBoolean isBreak, AtomicLong matchs, AtomicInteger waitSeachNum, File indexFile,
			SearchCallback callBack, String host, long total) {
		worker.execute(() -> {
			try {
				Query query = callBack.getQuery();
				Set<String> feilds = callBack.getFeilds();
				IndexSearcher searcher = getCachedSearch(indexFile, host);
				TopDocs docs = searcher.search(query, callBack.getSize());
				matchs.getAndAdd(docs.totalHits);
				for (ScoreDoc scoreDoc : docs.scoreDocs) {
					Document doc = searcher.doc(scoreDoc.doc, feilds);
					String file = doc.get(DocField.FILE);
					String line = doc.get(DocField.LINE);
					if (!callBack.handle(host, file, line)) {
						isBreak.set(true);
						break;
					}
				}
			} catch (Exception e) {
				LOG.error("ansy sear error", e);
			} finally {
				if (isBreak.get() || waitSeachNum.decrementAndGet() == 0)
					callBack.onFinish(total, matchs.get());
			}
		});

	}

	@Override
	public void onSystemExit() {
		worker.shutdownNow();
	}

	public void search(SearchCallback search, long allDocs, int memMatch) {
		AtomicLong match = new AtomicLong(memMatch);
		AtomicBoolean isBreak = new AtomicBoolean();
		AtomicInteger waitSeachNum = new AtomicInteger();
		try {
			Map<String, Collection<File>> dirs = findMatchIndexDir(search);
			for (Entry<String, Collection<File>> item : dirs.entrySet()) {
				Collection<File> value = item.getValue();
				waitSeachNum.getAndAdd(value.size());
				String host = item.getKey();
				value.forEach(file -> ansySearch(isBreak, match, waitSeachNum, file, search, host, allDocs));
			}
		} catch (Exception e) {
			isBreak.set(true);
			LOG.error("search erro", e);
		} finally {
			if (isBreak.get() || waitSeachNum.get() == 0)
				search.onFinish(allDocs, match.get());
		}

	}

	private IndexSearcher getCachedSearch(File file, String host) {
		try {
			IndexSearcher searcher = openedDir.get(file);
			if (searcher != null && modifyMap.get(file) >= file.lastModified()) {
				return searcher;
			}
			// 锁住目录
			synchronized (file) {
				// 关闭过期目录
				if (searcher != null) {
					openedDir.remove(file).getIndexReader().close();
				}
				if (!openedDir.containsKey(file)) {
					LOG.info("start open index:{}", file);
					FSDirectory index = FSDirectory.open(file.toPath());
					DirectoryReader newReader = DirectoryReader.open(index);
					openedDir.put(file, new IndexSearcher(newReader));
					modifyMap.put(file, file.lastModified());
					LOG.info("sucess to open index:{}", file);
				}
				return openedDir.get(file);
			}
		} catch (Exception e) {
			throw new RuntimeException(e.toString(), e.getCause());
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
	public Map<String, Collection<File>> findMatchIndexDir(SearchCallback searcher) {
		long from = Utils.igroeHMSTime(searcher.getBegin()).getTime();
		long to = Utils.igroeHMSTime(searcher.getEnd()).getTime();
		Map<String, Collection<File>> map = new HashMap<>();
		Set<String> hosts = searcher.getHosts();
		if (hosts != null && !hosts.isEmpty()) {
			for (String host : hosts) {
				TreeMap<Long, File> indexs = indexDirMap.get(host);
				if (indexs != null) {
					Collection<File> values = indexs.subMap(from, true, to, true).values();
					removeNotIndexDir(values);
					if (values.size() > 0)
						map.put(host, values);
				}
			}
		} else {
			for (Entry<String, TreeMap<Long, File>> entry : indexDirMap.entrySet()) {
				Collection<File> values = entry.getValue().values();
				removeNotIndexDir(values);
				if (values.size() > 0)
					map.put(entry.getKey(), values);
			}
		}
		return map;
	}

	// @link DirectoryReader.indexExists
	private void removeNotIndexDir(Collection<File> next) {
		String prefix = IndexFileNames.SEGMENTS + "_";
		Iterator<File> it = next.iterator();
		while (it.hasNext()) {
			boolean find = false;
			File dir = it.next();
			for (String file : dir.list()) {
				if (file.startsWith(prefix)) {
					find = true;
					break;
				}
			}
			if (find == false)
				it.remove();
		}
	}

	public long count(SearchCallback search, ConcurrentHashMap<String, Long> json) {
		Map<String, Collection<File>> dirs = findMatchIndexDir(search);
		if (dirs.isEmpty())
			return 0;

		AtomicLong count = new AtomicLong();
		CountDownLatch lock = new CountDownLatch(dirs.size());
		for (Entry<String, Collection<File>> item : dirs.entrySet()) {
			for (File indexDir : item.getValue()) {
				worker.execute(() -> {
					try {
						String host = item.getKey();
						IndexSearcher searcher = getCachedSearch(indexDir, host);
						long mtatch = searcher.count(search.getQuery());
						count.getAndAdd(mtatch);
						json.put(host, mtatch);
						lock.countDown();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
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
