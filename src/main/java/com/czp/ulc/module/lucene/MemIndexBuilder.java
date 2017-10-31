/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.module.lucene;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.core.message.MessageListener;
import com.czp.ulc.module.conn.ReadResult;
import com.czp.ulc.module.lucene.search.QueryBuilder;
import com.czp.ulc.module.lucene.search.SearchResult;
import com.czp.ulc.module.lucene.search.SearchTask;
import com.czp.ulc.util.Utils;
import com.czp.ulc.web.QueryCondtion;

/**
 * Function:创建log索引,支持实时搜索
 *
 * @date:2017年3月22日/下午4:52:47
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class MemIndexBuilder implements MessageListener<ReadResult> {

	private Analyzer analyzer;
	private DirectoryReader ramReader;
	private volatile IndexWriter ramWriter;
	private FileIndexBuilder fileIndexBuilder;
	private static final Logger LOG = LoggerFactory.getLogger(MemIndexBuilder.class);

	public MemIndexBuilder(FileIndexBuilder fileBuilder, Analyzer analyzer) {
		try {
			this.analyzer = analyzer;
			ramWriter = createRAMIndexWriter();
			ramReader = DirectoryReader.open(ramWriter);
			fileIndexBuilder = fileBuilder;
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
		fileIndexBuilder.shutdown();
		Utils.close(ramWriter);
	}

	@Override
	public boolean onMessage(ReadResult event, Map<String, Object> ext) {
		try {
			long now = System.currentTimeMillis();
			LOG.debug("recive message:{}", event);

			String file = event.getFile();
			String line = event.getLine();
			String host = event.getHost().getName();
			if (file.isEmpty() || line.isEmpty()) {
				LOG.info("empty file:[{}] line:[{}]", file, line);
				return true;
			}

			if (fileIndexBuilder.checkHasFlush()) {
				swapRamWriterReader();
			}

			addMemoryIndex(now, file, host, line);
			fileIndexBuilder.write(host, file, line, now);
			long end = System.currentTimeMillis();
			LOG.debug("add index time:{}ms", (end - now));
			return true;
		} catch (Exception e) {
			LOG.error("proces message error", e);
		}
		return true;
	}

	public void addMemoryIndex(long time, String file, String host, String line) throws IOException {
		Document doc = new Document();
		doc.add(new LongPoint(DocField.TIME, time));
		doc.add(new TextField(DocField.LINE, line, Field.Store.YES));
		doc.add(new TextField(DocField.FILE, file, Field.Store.YES));
		doc.add(new StringField(DocField.HOST, host, Field.Store.YES));
		ramWriter.addDocument(doc);

	}

	/***
	 * 在内存中搜索
	 * 
	 * @param task
	 * @param docCount
	 * @return
	 * @throws IOException
	 */
	public int searchInRam(SearchTask task) throws IOException {
		QueryCondtion cdt = task.getQuery();
		Query query = QueryBuilder.getMemQuery(analyzer, cdt);
		int size = cdt.getSize();

		IndexSearcher ramSearcher = getRamSearcher();
		TopDocs docs = ramSearcher.search(query, size);
		Set<String> feilds = cdt.getFeilds();
		int total = docs.totalHits;
		for (ScoreDoc scoreDoc : docs.scoreDocs) {
			Document doc = ramSearcher.doc(scoreDoc.doc, feilds);
			SearchResult res = new SearchResult();
			res.setFile(doc.get(DocField.FILE));
			res.setLine(doc.get(DocField.LINE));
			res.setHost(doc.get(DocField.HOST));
			res.setMatchCount(total);
			res.setFinish(size-- > 0);
			task.getCallback().handle(res);
			if (size <= 0) {
				break;
			}
		}
		LOG.info("query {} return:{} in ram", query, docs.totalHits);
		return total;
	}

	private IndexSearcher getRamSearcher() throws IOException {
		DirectoryReader openIfChanged = DirectoryReader.openIfChanged(ramReader);
		return new IndexSearcher(openIfChanged == null ? ramReader : openIfChanged);
	}

	private synchronized void swapRamWriterReader() throws IOException {
		DirectoryReader lastReader = ramReader;
		IndexWriter lastWriter = ramWriter;
		ramWriter = createRAMIndexWriter();
		ramReader = DirectoryReader.open(ramWriter);
		Utils.close(lastReader, lastWriter);
		LOG.info("success to swap ram wirter");

	}

	private IndexWriter createRAMIndexWriter() throws IOException {
		IndexWriterConfig conf = new IndexWriterConfig(analyzer);
		return new IndexWriter(new RAMDirectory(), conf);
	}

	public Map<String, Long> count(SearchTask search) throws IOException {
		long st = System.currentTimeMillis();
		QueryCondtion query = search.getQuery();
		IndexSearcher ramSearcher = getRamSearcher();
		TopDocs result = ramSearcher.search(QueryBuilder.getMemQuery(analyzer, query), Integer.MAX_VALUE);
		ConcurrentHashMap<String, Long> json = new ConcurrentHashMap<>();
		for (ScoreDoc did : result.scoreDocs) {
			Document doc = ramSearcher.doc(did.doc);
			String host = doc.get(DocField.HOST);
			Long pv = (Long) json.get(host);
			if (pv != null) {
				json.put(host, pv + 1);
			} else {
				json.put(host, 1L);
			}
		}
		long end = System.currentTimeMillis();
		LOG.info("count in ram time:{}", (end - st));
		return json;
	}
}
