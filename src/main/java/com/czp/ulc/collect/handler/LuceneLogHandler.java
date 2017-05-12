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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.collect.ReadResult;
import com.czp.ulc.common.MessageListener;
import com.czp.ulc.common.lucene.MyAnalyzer;
import com.czp.ulc.common.meta.DataMeta;
import com.czp.ulc.common.meta.MetaReadWriter;
import com.czp.ulc.common.util.Utils;

/**
 * Function:创建log索引,支持实时搜索
 *
 * @date:2017年3月22日/下午4:52:47
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class LuceneLogHandler implements MessageListener<ReadResult> {

	private MetaReadWriter readWriter;
	private volatile IndexWriter ramWriter;
	private Analyzer analyzer = new MyAnalyzer();
	public static final String FORMAT = "yyyyMMdd";

	private DataMeta meta;
	/*** 根目录 */
	private static final File ROOT = new File("./log");
	/** 已压缩文件目录 */
	private static final File ZIP_DIR = new File(ROOT, "zip");
	/** 索引根目录 */
	private static final File INDEX_DIR = new File(ROOT, "index");
	/** 未压缩文件目录 */
	private static final File DATA_DIR = new File(ROOT, "data");

	private ConcurrentHashMap<File, Long> modifyMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<File, IndexSearcher> openedDir = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, TreeMap<Long, File>> indexDirMap = new ConcurrentHashMap<>();

	private static final Logger LOG = LoggerFactory.getLogger(LuceneLogHandler.class);

	public LuceneLogHandler() {
		DATA_DIR.mkdirs();
		INDEX_DIR.mkdirs();
		ZIP_DIR.mkdirs();
		ramWriter = createRAMIndexWriter();
		readWriter = new MetaReadWriter(DATA_DIR, ZIP_DIR, INDEX_DIR, this);
		meta = readWriter.loadMeta();
		loadAllIndexDir();
	}

	@Override
	public Class<ReadResult> processClass() {
		return ReadResult.class;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	@Override
	public void onExit() {
		Utils.close(ramWriter);
		Utils.close(readWriter);
	}

	@Override
	public boolean onMessage(ReadResult event, Map<String, Object> ext) {
		try {
			long now = System.currentTimeMillis();
			LOG.debug("recive message:{}", event);

			String file = event.getFile();
			String host = event.getHost().getName();
			List<String> lines = event.getLines();
			if (file.isEmpty()) {
				LOG.info("file name is empty:{}", lines);
				return false;
			}

			if (readWriter.checkHasCompress()) {
				swapRamWriterReader();
			}

			for (String line : lines) {
				line = line.trim();
				if (line.isEmpty())
					continue;

				meta.updateRAMLines(1);
				writerRAMDocument(now, file, host, line);
				readWriter.write(host, file, line, now);
			}
			long end = now = System.currentTimeMillis();
			LOG.debug("create index time:{}ms", (end - now));
			return false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public Document writerRAMDocument(long time, String file, String host, String line) throws IOException {
		Document doc = new Document();
		doc.add(new LongPoint(DocField.TIME, time));
		doc.add(new TextField(DocField.LINE, line, Field.Store.YES));
		doc.add(new TextField(DocField.FILE, file, Field.Store.YES));
		doc.add(new StringField(DocField.HOST, host, Field.Store.YES));
		ramWriter.addDocument(doc);
		return doc;
	}

	public long search(Searcher search, boolean loadMeta) throws Exception {

		long sum = meta.getDocs();
		int size = search.getSize();
		Set<String> hosts = search.getHosts();
		hosts = hosts == null ? indexDirMap.keySet() : hosts;

		AtomicInteger hasReturn = new AtomicInteger();
		sum += searchInRam(search, hasReturn, loadMeta);
		if (hasReturn.get() >= search.getSize())
			return sum;

		Map<String, Collection<File>> dirs = findMatchTimeDir(hosts, search.getBegin(), search.getEnd());
		for (Entry<String, Collection<File>> item : dirs.entrySet()) {
			for (File indexDir : item.getValue()) {
				IndexSearcher searcher = getOpenedSearcher(indexDir);
				searchInIndex(searcher, item.getKey(), search, hasReturn, loadMeta);
				sum += searcher.getIndexReader().numDocs();
				if (hasReturn.get() >= size)
					break;
			}
			if (hasReturn.get() >= size)
				break;
		}
		return sum;
	}

	public void loadAllIndexDir() {
		try {
			long count = 0;
			SimpleDateFormat sp = new SimpleDateFormat(FORMAT);
			for (File file : INDEX_DIR.listFiles()) {
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
					indexMap.put(sp.parse(index.getName()).getTime(), index);
				}
			}
			LOG.info("load all index,docs:{}", count);
		} catch (ParseException e) {
			LOG.error("ParseException error", e);
		}
	}

	private long searchInRam(Searcher search, AtomicInteger hasReturn, boolean loadMeta) throws IOException {
		Query query = search.getQuery();
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for (String host : search.getHosts()) {
			builder.add(new TermQuery(new Term(DocField.HOST, host)), Occur.MUST);
		}
		builder.add(query, Occur.MUST);
		BooleanQuery bQuery = builder.build();

		IndexSearcher ramSearcher = new IndexSearcher(DirectoryReader.open(ramWriter));
		TopDocs docs = ramSearcher.search(bQuery, search.getSize() - hasReturn.get());
		long allDoc = ramSearcher.getIndexReader().numDocs();
		int totalHits = docs.totalHits;
		for (ScoreDoc scoreDoc : docs.scoreDocs) {
			Document doc = ramSearcher.doc(scoreDoc.doc);
			String file = doc.get(DocField.FILE);
			String host = doc.get(DocField.HOST);
			String line = loadMeta ? doc.get(DocField.LINE) : null;
			search.handle(host, file, line, totalHits, meta);
			hasReturn.getAndDecrement();
		}
		ramSearcher.getIndexReader().close();
		LOG.info("query:{} return:{} in ram", bQuery, totalHits);
		return allDoc;
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
		if (hosts != null && !hosts.isEmpty()) {
			for (String host : hosts) {
				TreeMap<Long, File> indexs = indexDirMap.get(host);
				if (indexs != null)
					map.put(host, indexs.subMap(from, true, to, true).values());
			}
		} else {
			Set<Entry<String, TreeMap<Long, File>>> entrySet = indexDirMap.entrySet();
			for (Entry<String, TreeMap<Long, File>> entry : entrySet) {
				map.put(entry.getKey(), entry.getValue().values());
			}
		}
		return map;
	}

	private IndexSearcher getOpenedSearcher(File file) throws IOException {
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
	}

	private int searchInIndex(IndexSearcher searcher, String host, Searcher search, AtomicInteger hasReturn,
			boolean loadMeta) throws Exception {
		TopDocs docs = searcher.search(search.getQuery(), search.getSize() - hasReturn.get());
		hasReturn.addAndGet(docs.totalHits);
		for (ScoreDoc scoreDoc : docs.scoreDocs) {
			Document doc = searcher.doc(scoreDoc.doc);
			String file = doc.get(DocField.FILE);
			String data = loadDataFiled(loadMeta, doc);
			search.handle(host, file, data, hasReturn.get(), meta);
		}
		return docs.totalHits;
	}

	private String loadDataFiled(boolean loadMeta, Document doc) {
		if (loadMeta == false)
			return null;
		String data = doc.get(DocField.LINE);
		if (data != null)
			return data;
		int offset = Integer.valueOf(doc.get(DocField.OFFSET));
		int fileId = Integer.valueOf(doc.get(DocField.META_FILE));
		return readWriter.readLine(fileId, offset);
	}

	private void swapRamWriterReader() throws IOException {
		IndexWriter lastWriter = ramWriter;
		ramWriter = createRAMIndexWriter();
		lastWriter.close();
	}

	private IndexWriter createRAMIndexWriter() {
		try {
			RAMDirectory ramd = new RAMDirectory();
			IndexWriterConfig conf = new IndexWriterConfig(analyzer);
			IndexWriter indexWriter = new IndexWriter(ramd, conf);
			return indexWriter;
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}
}
