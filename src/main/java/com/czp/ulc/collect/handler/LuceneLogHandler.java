/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.collect.handler;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.collect.ReadResult;
import com.czp.ulc.common.MessageListener;
import com.czp.ulc.common.ThreadPools;
import com.czp.ulc.common.kv.KVDB;
import com.czp.ulc.common.kv.LevelDB;
import com.czp.ulc.common.lucene.MyAnalyzer;
import com.czp.ulc.common.meta.MetaReadWriter;
import com.czp.ulc.common.util.Utils;

/**
 * Function:创建log索引,支持实时搜索
 *
 * @date:2017年3月22日/下午4:52:47
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class LuceneLogHandler implements MessageListener<ReadResult>, Runnable {

	private volatile long lastTime = 0;
	private volatile long lastDocs = 0;
	private long hasCommitDocCount = 0;

	/** 索引文件目录名格式 */
	private String nameFormat = "yyyy-MM-dd";
	/*** 索引文件map {hostName:{time:indexFile}} */
	private MetaReadWriter metaWriter;
	private Analyzer analyzer = new MyAnalyzer();
	/** 总行数 */
	private AtomicLong lineCount = new AtomicLong();
	private HashMap<String, TreeMap<Long, File>> indexDirMap = new HashMap<>();
	/** 需要commit的index,每隔几秒同步一次 */
	private BlockingQueue<JSONObject> commitWriter = new LinkedBlockingQueue<>();
	/*** 已经打开的writer */
	private ConcurrentHashMap<File, IndexWriter> writers = new ConcurrentHashMap<>();
	/** 已经打开的目录的search */
	private ConcurrentHashMap<File, IndexSearcher> openedDir = new ConcurrentHashMap<>();
	/** 标记search的状态,处理多线程下关闭的问题,状态为1为需要关闭 */
	private ConcurrentHashMap<IndexSearcher, Integer> searchFlag = new ConcurrentHashMap<>();

	/*** 根目录 */
	private static final File ROOT = new File("./log");
	/** 索引根目录 */
	private static final File INDEX_DIR = new File(ROOT, "index");
	/** 索引根目录 */
	private static final File DATA_DIR = new File(ROOT, "data");
	/** 记录行数的KEY */
	private static final String LINE_COUNT_KEY = "LINE_COUNT_KEY";
	/** meta信息 */
	private KVDB meta = LevelDB.open(new File(ROOT, "meta").getAbsolutePath());

	private static final Logger LOG = LoggerFactory.getLogger(LuceneLogHandler.class);
	private static final Integer STATUS_NOTHONG = -1;
	private static final Integer STATUS_USING = 0;
	private static final Integer STATUS_CLOSE = 1;

	public static interface DocFieldConst {
		String TIME = "t";
		String FILE = "f";
		String LINE = "l";
		String metaId = "m";
		String[] ALL_FEILD = { TIME, FILE, LINE, metaId };
	}

	public LuceneLogHandler() {
		try {
			INDEX_DIR.mkdirs();
			DATA_DIR.mkdirs();
			loadAllIndexDir();
			metaWriter = new MetaReadWriter(DATA_DIR.toString());
			lineCount.set(metaWriter.loadLineCount(DATA_DIR));
			ThreadPools.getInstance().startThread("lucene-index", this, true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void loadAllIndexDir() throws ParseException {
		long count = 0;
		SimpleDateFormat sp = new SimpleDateFormat(nameFormat);
		for (File file : INDEX_DIR.listFiles()) {
			// 第一层为主机名
			String host = file.getName();
			TreeMap<Long, File> indexMap = indexDirMap.get(host);
			if (indexMap == null) {
				indexMap = new TreeMap<>();
				indexDirMap.put(host, indexMap);
			}
			for (File index : file.listFiles()) {
				// 第二层为时间目录 包含索引和数据目录
				indexMap.put(sp.parse(index.getName()).getTime(), index);
				count += meta.getLong(index.getAbsolutePath(), 0);
			}
		}
		hasCommitDocCount = count;
		LOG.info("load all index,docs:{}", count);
	}

	/***
	 * 找到当前的索引分片目录
	 * 
	 * @param host
	 * 
	 * @return
	 */
	private File findTodayIndexDir(String host, long time, SimpleDateFormat sdf) {
		File hostDir = new File(INDEX_DIR, host);
		hostDir.mkdirs();
		return new File(hostDir, sdf.format(Utils.igroeHMSTime(time)));
	}

	@Override
	public boolean onMessage(ReadResult event, Map<String, Object> ext) {
		try {
			long now = System.currentTimeMillis();
			LOG.debug("recive message:{}", event);

			String file = event.getFile();
			List<String> lines = event.getLines();
			if (file.isEmpty()) {
				LOG.info("file name is empty:{}", lines);
				return false;
			}
			String host = event.getHost().getName();
			SimpleDateFormat sdf = new SimpleDateFormat(nameFormat);
			File dir = findTodayIndexDir(host, now, sdf);
			IndexWriter writer = getIndexWriter(dir, sdf);

			for (String line : lines) {
				line = line.trim();
				if (line.length() == 0) {
					continue;
				}
				byte[] metaId = metaWriter.write(line);
				Document doc = new Document();
				doc.add(new LongPoint(DocFieldConst.TIME, now));
				doc.add(new StoredField(DocFieldConst.metaId, metaId));
				doc.add(new TextField(DocFieldConst.LINE, line, Field.Store.NO));
				doc.add(new TextField(DocFieldConst.FILE, file, Field.Store.YES));
				writer.addDocument(doc);
				lineCount.getAndIncrement();
			}
			long nowDocs = writer.numDocs();
			notifyCommitIndex(writer, dir, nowDocs);
			LOG.debug("create index time:{}ms docs:{}", (System.currentTimeMillis() - now), lastDocs);
		} catch (Exception e) {
			LOG.error("process message error", e);
		}
		return false;
	}

	private void notifyCommitIndex(IndexWriter writer, File indexDir, long nowDocs) throws Exception {
		long now = System.currentTimeMillis();
		if ((now - lastTime) > 5000 || nowDocs - lastDocs >= 200) {
			JSONObject task = new JSONObject();
			task.put("writer", writer);
			task.put("file", indexDir);
			commitWriter.put(task);
			lastDocs = nowDocs;
			lastTime = now;
		}
	}

	@Override
	public void onExit() {
		try {
			for (Entry<File, IndexWriter> entry : writers.entrySet()) {
				IndexWriter value = entry.getValue();
				if (value.hasUncommittedChanges())
					value.commit();
				if (value.isOpen())
					Utils.close(value);
			}
			openedDir.values().stream().forEach(item -> Utils.close(item.getIndexReader()));
		} catch (Exception e) {
			LOG.error("exit error", e);
		}
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	/***
	 * 
	 * @param search
	 * @param loadMeta
	 * @return
	 * @throws Exception
	 */
	public long search(Searcher search, boolean loadMeta) throws Exception {

		int size = search.getSize();
		long sum = hasCommitDocCount + lastDocs;

		Set<String> hosts = search.getHosts();
		hosts = hosts == null ? indexDirMap.keySet() : hosts;

		AtomicLong docs = new AtomicLong();
		AtomicInteger hasAddSize = new AtomicInteger();
		Map<String, Collection<File>> dirs = findMatchTimeDir(hosts, search.getBegin(), search.getEnd());
		for (Entry<String, Collection<File>> item : dirs.entrySet()) {
			String host = item.getKey();
			for (File index : item.getValue()) {
				searchInIndex(getOpenedSearcher(index), index, host, search, docs, hasAddSize, loadMeta);
				if (hasAddSize.get() >= size)
					break;
			}
			if (hasAddSize.get() >= size)
				break;
		}
		return sum;

	}

	/****
	 * 查询某个文件的总数
	 * 
	 * @param file
	 * @param hosts
	 * @param start
	 * @param end
	 * @return
	 * @throws Exception
	 */
	public JSONObject count(String file, Set<String> hosts, long start, long end) throws Exception {
		long timeStart = System.currentTimeMillis();

		if (hosts == null) {
			hosts = indexDirMap.keySet();
		}

		long total = 0;
		JSONObject eachHost = new JSONObject();
		TermQuery query = new TermQuery(new Term(DocFieldConst.FILE, file));
		Map<String, Collection<File>> dirs = findMatchTimeDir(hosts, start, end);
		for (Entry<String, Collection<File>> entry : dirs.entrySet()) {
			long count = 0;
			for (File indexDir : entry.getValue()) {
				count += getOpenedSearcher(indexDir).count(query);
			}
			total += count;
			eachHost.put(entry.getKey(), count);
		}
		long timeEnd = System.currentTimeMillis();
		double time = (timeEnd - timeStart) / 1000.0;
		JSONObject result = new JSONObject();
		result.put("end", end);
		result.put("time", time);
		result.put("total", total);
		result.put("start", start);
		result.put("host", eachHost);
		return result;

	}

	/**
	 * 找到匹配时间点的索引目录
	 * 
	 * @param hosts
	 * @param start
	 * @param end
	 * @return
	 */
	public Map<String, Collection<File>> findMatchTimeDir(Set<String> hosts, long start, long end) {
		long from = Utils.igroeHMSTime(start).getTime();
		long to = Utils.igroeHMSTime(end).getTime();
		Map<String, Collection<File>> map = new HashMap<>();
		for (String host : hosts) {
			TreeMap<Long, File> indexs = indexDirMap.get(host);
			if (indexs != null)
				map.put(host, indexs.subMap(from, true, to, true).values());
		}
		return map;
	}

	private IndexSearcher getOpenedSearcher(File file) throws IOException {
		IndexSearcher reader = openedDir.get(file);
		if (reader != null)
			return reader;

		// 锁住父目录
		synchronized (file) {
			if (!openedDir.containsKey(file)) {
				LOG.info("start open index:{}", file);
				FSDirectory open = FSDirectory.open(file.toPath());
				DirectoryReader newReader = DirectoryReader.open(open);
				openedDir.put(file, new IndexSearcher(newReader));
				LOG.info("sucess to open index:{}", file);
			}
			return openedDir.get(file);
		}
	}

	private void searchInIndex(IndexSearcher searcher, File dir, String host, Searcher search, AtomicLong docsCount,
			AtomicInteger hasAddSize, boolean loadMeta) throws Exception {

		// 标记当前的searcher正在使用,不用关闭
		synchronized (searcher) {
			searchFlag.put(searcher, STATUS_USING);
		}
		List<Document> matchDocs = new LinkedList<>();
		TopDocs docs = searcher.search(search.getQuery(), search.getSize() - hasAddSize.get());
		for (ScoreDoc scoreDoc : docs.scoreDocs) {
			matchDocs.add(searcher.doc(scoreDoc.doc));
		}

		// 如果当前searcher已经被commit线程标记为关闭,则关闭
		synchronized (searcher) {
			if (searchFlag.getOrDefault(searcher, STATUS_NOTHONG) == STATUS_CLOSE) {
				searcher.getIndexReader().close();
				openedDir.remove(dir);
			}
		}

		int totalHits = docs.totalHits;
		docsCount.getAndAdd(totalHits);
		if (loadMeta == false) {
			for (Document doc : matchDocs) {
				String file = doc.get(DocFieldConst.FILE);
				search.handle(host, file, null, docsCount.get(), lineCount.get());
				hasAddSize.getAndIncrement();
			}
			return;
		}

		List<byte[]> lineRequest = new LinkedList<>();
		for (Document doc : matchDocs) {
			lineRequest.add(doc.getBinaryValue(DocFieldConst.metaId).bytes);
		}
		Map<Long, Map<Long, String>> linesMap = metaWriter.mergeRead(lineRequest);
		for (Document doc : matchDocs) {
			byte[] metaId = doc.getBinaryValue(DocFieldConst.metaId).bytes;
			long[] meta = MetaReadWriter.decodeMetaId(metaId);
			long uuid = meta[0];
			long lineId = meta[1];
			String file = doc.get(DocFieldConst.FILE);
			String line = linesMap.get(uuid).get(lineId);
			search.handle(host, file, line, docsCount.get(), lineCount.get());
			hasAddSize.getAndIncrement();
		}
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			try {
				JSONObject task = commitWriter.take();
				long st = System.currentTimeMillis();
				File indexDir = (File) task.get("file");
				IndexWriter writer = (IndexWriter) task.get("writer");
				writer.commit();

				long numDocs = writer.numDocs();
				meta.put(indexDir.getAbsolutePath(), numDocs);
				meta.put(LINE_COUNT_KEY, lineCount.get());
				long end = System.currentTimeMillis();
				long commitTime = end - st;
				// 先put新的reader到map,再关闭老的reader
				IndexSearcher search = openedDir.get(indexDir);
				IndexReader newReader = DirectoryReader.open(writer);
				openedDir.put(indexDir, new IndexSearcher(newReader));

				// 如果search正在使用,只是标记为close,搜索线程用完后关闭
				if (searchFlag.containsKey(search)) {
					searchFlag.put(search, STATUS_CLOSE);
				} else {
					Utils.close(search.getIndexReader());
				}
				long reopenTime = System.currentTimeMillis() - end;
				LOG.debug(String.format("commit:%sms,reopen reader:%sms docs:%s", commitTime, reopenTime, numDocs));
			} catch (Exception e) {
				LOG.error("write to file error", e);
			}
		}
	}

	private synchronized IndexWriter getIndexWriter(File file, SimpleDateFormat sdf) throws Exception {
		if (writers.containsKey(file)) {
			return writers.get(file);
		}
		closeExpireIndexWriter(sdf);
		return createAndCacneIndexWriter(file, sdf);
	}

	private IndexWriter createAndCacneIndexWriter(File file, SimpleDateFormat sdf) throws Exception {
		TieredMergePolicy mergePolicy = new TieredMergePolicy();
		IndexWriterConfig conf = new IndexWriterConfig(analyzer);
		conf.setOpenMode(OpenMode.CREATE_OR_APPEND);
		conf.setMergePolicy(mergePolicy);
		conf.setUseCompoundFile(true);

		IndexWriter writer = new IndexWriter(FSDirectory.open(file.toPath()), conf);
		DirectoryReader reader = DirectoryReader.open(writer);
		openedDir.put(file, new IndexSearcher(reader));
		writers.put(file, writer);

		refreshIndexDirMap(sdf.parse(file.getName()).getTime(), file, writer.numDocs());
		return writer;
	}

	private void refreshIndexDirMap(Long time, File file, long docs) {
		try {
			String host = file.getParentFile().getName();
			TreeMap<Long, File> indexMap = indexDirMap.get(host);
			if (indexMap == null) {
				indexMap = new TreeMap<>();
				indexDirMap.put(host, indexMap);
			}
			indexMap.put(time, file);
			hasCommitDocCount += docs;
		} catch (Exception e) {
			LOG.error("refresh index dir error", e);
		}
	}

	private void closeExpireIndexWriter(SimpleDateFormat sdf) throws ParseException {
		long now = Utils.igroeHMSTime(System.currentTimeMillis()).getTime();
		for (Entry<File, IndexWriter> entry : writers.entrySet()) {
			File file = entry.getKey();
			long fileTime = sdf.parse(file.getName()).getTime();
			if (fileTime >= now)
				continue;

			// 关闭索引
			IndexWriter remove = writers.remove(file);
			// 先remove再关闭,避免关闭后搜索线程还使用这个writer
			if (remove.hasUncommittedChanges()) {
				try {
					remove.commit();
				} catch (IOException e) {
					LOG.info("commit error", e);
				}
			}
			Utils.close(remove);
			LOG.info("sucess to close expire index:{}", file);
		}
	}

	@Override
	public Class<ReadResult> processClass() {
		return ReadResult.class;
	}
}
