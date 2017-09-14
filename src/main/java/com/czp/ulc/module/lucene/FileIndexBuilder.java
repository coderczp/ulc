package com.czp.ulc.module.lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.codecs.lucene50.Lucene50StoredFieldsFormat.Mode;
import org.apache.lucene.codecs.lucene62.Lucene62Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.core.ThreadPools;
import com.czp.ulc.core.bean.IndexMeta;
import com.czp.ulc.core.bean.LuceneFile;
import com.czp.ulc.core.dao.IndexMetaDao;
import com.czp.ulc.core.dao.LuceneFileDao;
import com.czp.ulc.util.Utils;

/**
 * 请添加描述 <br/>
 * <li>创建人：Jeff.cao</li><br>
 * <li>创建时间：2017年5月3日 下午12:40:14</li>
 * 
 * @version 0.0.1
 */
public class FileIndexBuilder {

	private File dataDir;
	private File indexBaseDir;

	private Analyzer analyzer;
	private RollingWriter writer;
	private IndexMetaDao metaDao;
	private LuceneFileDao lFileDao;

	private ExecutorService worker;
	private AtomicLong lineCount = new AtomicLong();
	private AtomicBoolean hasFlushed = new AtomicBoolean();
	private ConcurrentHashMap<File, IndexWriter> indexMap = new ConcurrentHashMap<>();

	public static final String HOST = Utils.getHostName();
	public static final Charset UTF8 = Charset.forName("UTF-8");
	public static final String LINE_SPLITER = Utils.getLineSpliter();
	private static final Logger log = LoggerFactory.getLogger(FileIndexBuilder.class);

	/***
	 * 
	 * @param srcDir 原始文件的存放目录
	 * @param indexDir 索引文件目录
	 * @param analyzer 索引分析器
	 * @param metaDao  meta信息dao
	 * @param lFileDao Lucene文件dao
	 */
	public FileIndexBuilder(File srcDir, File indexDir, Analyzer analyzer, IndexMetaDao metaDao,
			LuceneFileDao lFileDao) {
		this.metaDao = metaDao;
		this.dataDir = srcDir;
		this.lFileDao = lFileDao;
		this.analyzer = analyzer;
		this.indexBaseDir = indexDir;
		this.writer = new RollingWriter(dataDir);
		this.worker = ThreadPools.getInstance().newPool("lucene-file-index", 1);

		this.repairFailFile(srcDir);
	}

	/***
	 * 修复因为进程异常退出没有commit的数据
	 * 
	 * @param baseDir
	 */
	private void repairFailFile(File baseDir) {
		LinkedList<File> files = new LinkedList<File>();
		File curFile = writer.getCurrentFile();
		for (File file : writer.getAllFiles()) {
			if (file.equals(curFile))
				continue;
			files.add(file);
		}
		asynRepair(files);
	}

	/**
	 * 异步修复未提交的索引
	 * @param files
	 */
	private void asynRepair(final List<File> files) {
		log.info("wait repair file size:{}", files.size());
		ThreadPools.getInstance().run("repair-thread", () -> {
			for (File file : files) {
				log.info("repair log file:{}", file);
				try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
					String readLine;
					long now = System.currentTimeMillis();
					while ((readLine = br.readLine()) != null) {
						String[] data = decodeData(readLine);
						if (data == null) {
							log.error("fail to decode:{}",readLine);
							continue;
						}
						String host = data[1];
						String line = data[3];
						String srcFile = data[2];
						long time = Long.valueOf(data[0]);
						doAddIndex(host, srcFile, line, time);
					}
					boolean delete = file.delete();
					long end = System.currentTimeMillis();
					log.info("repair:{} time:{}ms delete:{}", file, (end - now), delete);
				} catch (Exception e) {
					log.error("repair error:"+file, e);
				}
			}
		}, true);
	}

	/****
	 * 滚动写文件
	 * 
	 * @param host
	 * @param file
	 * @param line
	 * @param time
	 * @return
	 * @throws Exception
	 */
	public boolean write(String host, String file, String line, long time) throws Exception {
		String string = encodeData(host, file, line, time);
		RollingWriterResult result = writer.append(string.getBytes(UTF8));
		asynAddDoc(host, file, line, time, result);
		return result.isFileChanged();
	}

	/***
	 * 将主机文件名日志时间都编码到一行,异常退出时才能修复
	 * @param host
	 * @param file
	 * @param line
	 * @param now
	 * @return
	 */
	private String encodeData(String host, String file, String line, long now) {
		String time = String.valueOf(now);
		int strLen = host.length() + file.length() + line.length() + time.length();
		StringBuilder sb = new StringBuilder(strLen + 3);
		sb.append(now).append("#");
		sb.append(host).append("*");
		sb.append(file).append("@");
		sb.append(line).append(LINE_SPLITER);
		return sb.toString();
	}

	private String[] decodeData(String line) {
		int indexTime = line.indexOf("#");
		int indexHost = line.indexOf("*");
		int indexFile = line.indexOf("@");
		if (indexTime < 0 || indexHost < 0 || indexFile < 0) {
			log.error("error line:{}", line);
			return null;
		}
		String time = line.substring(0, indexTime);
		String host = line.substring(indexTime + 1, indexHost);
		String srcFile = line.substring(indexHost + 1, indexFile);
		String data = line.substring(indexFile + 1, line.length());
		return new String[] { time, host, srcFile, data };
	}

	// 异步添加document
	private void asynAddDoc(String host, String file, String line, long now, RollingWriterResult result) {
		worker.execute(() -> {
			try {
				doAddIndex(host, file, line, now);
				if (result.isFileChanged()) {
					commitIndex(result.getLastFile());
				}
			} catch (Exception e) {
				log.error("add doc error", e);
			}
		});
	}

	private void doAddIndex(String host, String file, String line, long now) throws IOException {
		lineCount.getAndIncrement();
		Date day = Utils.toDay(now);
		SimpleDateFormat sp = new SimpleDateFormat(LuceneConfig.FORMAT);

		Document doc = new Document();
		doc.add(new LongPoint(DocField.TIME, now));
		doc.add(new TextField(DocField.LINE, line, Field.Store.YES));
		doc.add(new TextField(DocField.FILE, file, Field.Store.YES));

		File indexDir = createIndexDir(sp.format(day), host, day);
		IndexWriter writer = getIndexWriter(analyzer, indexDir);
		// 防止被flush线程关闭
		if (!writer.isOpen()) {
			writer = getIndexWriter(analyzer, indexDir);
		}
		writer.addDocument(doc);
	}

	/***
	 * 更新统计信息:行数 原始字节数
	 * 
	 * @param bytes
	 * @param lineCount
	 * @throws IOException
	 */
	protected void updateMetaInfo(long bytes, long lines, long docs) {
		IndexMeta meta = new IndexMeta();
		try {
			meta.setBytes(bytes);
			meta.setLines(lines);
			meta.setDocs(docs);
			metaDao.add(meta);
			log.info("sucess to write meta:{}", meta);
		} catch (Exception e) {
			log.error("fail to write meta" + meta, e);
		}
	}

	/***
	 * 根据日期创建索引目录
	 * 
	 * @param date
	 * @param host
	 * @return
	 */
	private File createIndexDir(String dateStr, String server, Date date) {
		File indexDir = new File(indexBaseDir, String.format("%s/%s", server, dateStr));
		if (!indexDir.exists()) {
			indexDir.mkdirs();
			// 当前文件夹是新建的,添加数据库记录
			saveLuceneIndexFileToDb(server, date, indexDir);
		}
		return indexDir;
	}

	private void saveLuceneIndexFileToDb(String server, Date date, File indexDir) {
		LuceneFile bean = new LuceneFile();
		bean.setHost(HOST);
		bean.setServer(server);
		bean.setItime(date.getTime());
		bean.setPath(indexDir.getAbsolutePath());
		int ret = lFileDao.insert(bean);
		if (ret < 1) {
			log.error("fail to save lucene file {}", bean);
		}
	}

	protected long flushIndex(Map<File, IndexWriter> indexMap) {
		long count = 0;
		log.info("start flush index:{}", indexMap.size());
		for (Entry<File, IndexWriter> entry : indexMap.entrySet()) {
			IndexWriter value = entry.getValue();
			try {
				long st = System.currentTimeMillis();
				value.commit();
				count += value.numDocs();
				indexMap.remove(entry.getKey()).close();
				long end = System.currentTimeMillis();
				log.info("commit index time:{}", (end - st));
			} catch (Exception e) {
				log.error("fail to close index writer:" + value, e);
			}
		}
		return count;
	}

	protected IndexWriter getIndexWriter(Analyzer analyzer, File indexDir) {
		IndexWriter writer = indexMap.get(indexDir);
		if (writer == null) {
			writer = createIndexWriter(analyzer, indexDir);
			indexMap.put(indexDir, writer);
		}
		return writer;
	}

	private IndexWriter createIndexWriter(Analyzer analyzer, File file) {
		try {
			TieredMergePolicy mergePolicy = new TieredMergePolicy();
			IndexWriterConfig conf = new IndexWriterConfig(analyzer);
			conf.setCodec(new Lucene62Codec(Mode.BEST_COMPRESSION));
			conf.setOpenMode(OpenMode.CREATE_OR_APPEND);
			conf.setMergePolicy(mergePolicy);
			conf.setUseCompoundFile(true);
			return new IndexWriter(FSDirectory.open(file.toPath()), conf);
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	public void shutdown() {
		writer.close();
		worker.shutdown();
		long docCount = flushIndex(indexMap);
		updateMetaInfo(0, lineCount.getAndSet(0), docCount);
	}

	/**
	 * 标记上一个文件是否写入磁盘完成
	 * 
	 * @param b
	 * @return
	 */
	public boolean checkHasFlush() {
		return hasFlushed.getAndSet(false);
	}

	// 文件滚动时commit索引,完成后删除原文件
	private void commitIndex(File srcFile) {
		try {
			long length = srcFile.length();
			long docCount = flushIndex(indexMap);
			boolean srcDelete = srcFile.delete();
			hasFlushed.set(true);
			updateMetaInfo(length, lineCount.getAndSet(0), docCount);
			log.info("index file:{} size:{} del:{}", srcFile, length, srcDelete);
		} catch (Exception e) {
			log.error(e.toString(), e);
		}
	}

}
