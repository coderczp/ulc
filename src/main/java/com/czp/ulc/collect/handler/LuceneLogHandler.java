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
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
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
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.collect.ReadResult;
import com.czp.ulc.common.MessageListener;
import com.czp.ulc.common.ThreadPools;
import com.czp.ulc.common.kv.KVDB;
import com.czp.ulc.common.kv.LevelDB;
import com.czp.ulc.common.util.IdGnerator;
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

	/** 总行数 */
	private AtomicLong lineCount = new AtomicLong();

	private Analyzer analyzer = new StandardAnalyzer();

	private IdGnerator idFactory = IdGnerator.getInstance();

	private HashMap<String, TreeMap<Long, File>> indexDirMap = new HashMap<>();

	/** 需要commit的index,每隔几秒同步一次 */
	private BlockingQueue<JSONObject> commitWriter = new LinkedBlockingQueue<>();

	/*** 已经打开的writer */
	private ConcurrentHashMap<File, IndexWriter> writers = new ConcurrentHashMap<>();

	/** 已经打开的目录的search */
	private ConcurrentHashMap<File, IndexSearcher> openedDir = new ConcurrentHashMap<>();

	/*** 根目录 */
	private static final File ROOT = new File("./log");

	/** 索引根目录 */
	private static final File INDEX_DIR = new File(ROOT, "index");

	/** 记录行数的KEY */
	private static final String LINE_COUNT_KEY = "LINE_COUNT_KEY";

	/** meta信息 */
	private KVDB meta = LevelDB.open(new File(ROOT, "meta").getAbsolutePath());

	private static final Logger LOG = LoggerFactory.getLogger(LuceneLogHandler.class);

	public LuceneLogHandler() {
		try {
			INDEX_DIR.mkdirs();
			loadAllIndexDir();
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
	private File findCurrentIndexDir(String host, long time, SimpleDateFormat sdf) {
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
			if (file.isEmpty()) {
				LOG.info("file name is empty:{}", event.getLines());
				return false;
			}
			StringBuilder sb = new StringBuilder();
			for (String line : event.getLines()) {
				line = line.trim();
				if (line.length() == 0) {
					continue;
				}
				sb.append(line);
				lineCount.getAndIncrement();
			}

			if (sb.length() == 0)
				return true;

			String all = sb.toString();
			int nowId = idFactory.nextId(now);
			String host = event.getHost().getName();

			ByteBuffer data = ByteBuffer.allocate(Integer.BYTES + Long.BYTES);
			data.putLong(now);
			data.putInt(nowId);
			byte[] dataId = data.array();

			Document doc = new Document();
			doc.add(new LongPoint("time", now));
			doc.add(new StoredField("id", new BytesRef(dataId)));
			doc.add(new TextField("line", all, Field.Store.NO));
			doc.add(new TextField("file", file, Field.Store.YES));

			SimpleDateFormat sdf = new SimpleDateFormat(nameFormat);
			File dir = findCurrentIndexDir(host, now, sdf);

			IndexWriter writer = getIndexWriter(dir, sdf);
			writer.addDocument(doc);

			meta.put(dataId, all);

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
	 * @return
	 * @throws Exception
	 */
	public long search(Searcher search) throws Exception {

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
				searchInIndex(getOpenedSearcher(index), index, host, search, docs, hasAddSize);
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
		TermQuery query = new TermQuery(new Term("file", file));
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

	private int searchInIndex(IndexSearcher searcher, File dir, String host, Searcher search, AtomicLong docsCount,
			AtomicInteger hasAddSize) throws IOException {
		Set<String> fields = search.getFields();
		
		TopDocs docs = searcher.search(search.getQuery(), search.getSize() - hasAddSize.get());
		int totalHits = docs.totalHits;
		docsCount.getAndAdd(totalHits);

		String line = null;
		for (ScoreDoc scoreDoc : docs.scoreDocs) {
			Document doc = searcher.doc(scoreDoc.doc, fields);
			IndexableField field = doc.getField("id");
			if(field!=null){
				line = meta.get(field.binaryValue().bytes);
			}
			search.handle(host, doc, line, docsCount.get());
			hasAddSize.incrementAndGet();
		}
		return totalHits;
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
				// 先put新的reader到map,再关闭老的reader,搜索线程要捕获异常,如搜索时被关闭,需要重新去map取
				IndexSearcher search = openedDir.get(indexDir);
				IndexReader newReader = DirectoryReader.open(writer);
				openedDir.put(indexDir, new IndexSearcher(newReader));
				Utils.close(search.getIndexReader());
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
		LogByteSizeMergePolicy mergePolicy = new LogByteSizeMergePolicy();
		mergePolicy.setMergeFactor(5000);

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
