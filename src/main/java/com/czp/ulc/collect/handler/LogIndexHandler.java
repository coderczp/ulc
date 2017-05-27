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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.collect.ReadResult;
import com.czp.ulc.common.MessageListener;
import com.czp.ulc.common.bean.IndexMeta;
import com.czp.ulc.common.dao.IndexMetaDao;
import com.czp.ulc.common.lucene.ConcurrentSearch;
import com.czp.ulc.common.lucene.DocField;
import com.czp.ulc.common.lucene.LogAnalyzer;
import com.czp.ulc.common.meta.AsynIndexManager;
import com.czp.ulc.common.util.Utils;
import com.czp.ulc.main.Application;

/**
 * Function:创建log索引,支持实时搜索
 *
 * @date:2017年3月22日/下午4:52:47
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class LogIndexHandler implements MessageListener<ReadResult> {

	private DirectoryReader ramReader;
	private AsynIndexManager logWriter;
	private volatile IndexWriter ramWriter;
	private ConcurrentSearch concurrentSearch;
	private Analyzer analyzer = new LogAnalyzer();
	private AtomicLong nowLines = new AtomicLong();

	/** 索引文件目录日期格式 */
	public static final String FORMAT = "yyyyMMdd";
	/*** 根目录 */
	private static final File ROOT = new File("./log");
	/** 未压缩文件目录 */
	private static final File DATA_DIR = new File(ROOT, "data");
	/** 索引根目录 */
	private static final File INDEX_DIR = new File(ROOT, "index");

	private ConcurrentHashMap<String, TreeMap<Long, File>> indexDirMap = new ConcurrentHashMap<>();

	private static final Logger LOG = LoggerFactory.getLogger(LogIndexHandler.class);

	public LogIndexHandler() {
		try {
			DATA_DIR.mkdirs();
			INDEX_DIR.mkdirs();
			ramWriter = createRAMIndexWriter();
			ramReader = DirectoryReader.open(ramWriter);
			logWriter = new AsynIndexManager(DATA_DIR, INDEX_DIR, this);
			concurrentSearch = new ConcurrentSearch(Runtime.getRuntime().availableProcessors());
			loadAllIndexDir();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
		Utils.close(logWriter);
	}

	@Override
	public boolean onMessage(ReadResult event, Map<String, Object> ext) {
		try {
			long now = System.currentTimeMillis();
			LOG.debug("recive message:{}", event);

			String file = event.getFile();
			String line = event.getLine().trim();
			String host = event.getHost().getName();
			if (file.isEmpty() || line.isEmpty()) {
				LOG.info("empty file:[{}] line:[{}]", file, event.getLine());
				return false;
			}

			if (logWriter.checkHasFlush()) {
				swapRamWriterReader();
			}

			nowLines.getAndIncrement();
			addMemoryIndex(now, file, host, line);
			logWriter.write(host, file, line, now);
			long end = now = System.currentTimeMillis();
			LOG.debug("create index time:{}ms", (end - now));
			return false;
		} catch (Exception e) {
			LOG.error("proces message error", e);
		}
		return false;
	}

	public void addMemoryIndex(long time, String file, String host, String line) throws IOException {
		Document doc = new Document();
		doc.add(new LongPoint(DocField.TIME, time));
		doc.add(new TextField(DocField.LINE, line, Field.Store.YES));
		doc.add(new TextField(DocField.FILE, file, Field.Store.YES));
		doc.add(new StringField(DocField.HOST, host, Field.Store.YES));
		ramWriter.addDocument(doc);
	}

	public void search(SearchCallback search) throws Exception {

		long fileDocs = getMeta().getDocs() + ramReader.numDocs();
		if (searchInRam(search, fileDocs))
			return;
		concurrentSearch.search(search, indexDirMap, fileDocs);
	}

	public void loadAllIndexDir() {
		try {
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
			LOG.info("load all index");
		} catch (ParseException e) {
			LOG.error("ParseException error", e);
		}
	}

	private boolean searchInRam(SearchCallback search, long docCount) throws IOException {
		boolean stop = false;
		BooleanQuery bQuery = buildRamQuery(search);
		IndexSearcher ramSearcher = new IndexSearcher(DirectoryReader.openIfChanged(ramReader));
		TopDocs docs = ramSearcher.search(bQuery, search.getSize());
		for (ScoreDoc scoreDoc : docs.scoreDocs) {
			Document doc = ramSearcher.doc(scoreDoc.doc, search.getFeilds());
			String file = doc.get(DocField.FILE);
			String host = doc.get(DocField.HOST);
			String line = doc.get(DocField.LINE);
			if (!search.handle(host, file, line, docs.totalHits, docCount)) {
				stop = true;
				break;
			}
		}
		LOG.info("query:{} return:{} in ram", bQuery, docs.totalHits);
		return stop;
	}

	private BooleanQuery buildRamQuery(SearchCallback search) {
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		for (String host : search.getHosts()) {
			builder.add(new TermQuery(new Term(DocField.HOST, host)), Occur.MUST);
		}
		builder.add(search.getQuery(), Occur.MUST);
		builder.add(LongPoint.newRangeQuery(DocField.TIME, search.getBegin(), search.getEnd()), Occur.MUST);
		return builder.build();
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

	private synchronized void swapRamWriterReader() throws IOException {
		DirectoryReader lastReader = ramReader;
		IndexWriter lastWriter = ramWriter;
		ramWriter = createRAMIndexWriter();
		ramReader = DirectoryReader.open(ramWriter);
		Utils.close(lastReader);
		Utils.close(lastWriter);
		LOG.info("success to swap ram wirter");
	}

	private IndexWriter createRAMIndexWriter() throws IOException {
		IndexWriterConfig conf = new IndexWriterConfig(analyzer);
		return new IndexWriter(new RAMDirectory(), conf);
	}

	public JSONObject count(SearchCallback search) throws IOException {
		long allCount = 0;
		long st = System.currentTimeMillis();
		ConcurrentHashMap<String, Object> json = new ConcurrentHashMap<String, Object>();
		BooleanQuery bQuery = buildRamQuery(search);
		IndexSearcher ramSearcher = new IndexSearcher(DirectoryReader.openIfChanged(ramReader));
		TopDocs count = ramSearcher.search(bQuery, Integer.MAX_VALUE);
		ScoreDoc[] dosc = count.scoreDocs;
		for (ScoreDoc doc : dosc) {
			String host = ramSearcher.doc(doc.doc).get(DocField.HOST);
			Long pv = (Long) json.get(host);
			if (pv == null) {
				json.put(host, 1l);
			} else {
				json.put(host, pv + 1);
			}
			allCount++;
		}
		allCount += concurrentSearch.count(search,json,indexDirMap);
		json.put("all", allCount);
		long end = System.currentTimeMillis();
		LOG.info("count in ram time:{}", (end - st));
		return (JSONObject) JSONObject.toJSON(json);
	}

	public IndexMeta getMeta() {
		IndexMeta meta = Application.getBean(IndexMetaDao.class).count(null);
		nowLines.getAndAdd(meta.getLines());
		return meta;
	}
}
