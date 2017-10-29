package com.czp.ulc.module.lucene;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
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
import com.czp.ulc.module.conn.ProcessorUtil;
import com.czp.ulc.util.Utils;

/**
 * 请添加描述 <br/>
 * <li>创建人：Jeff.cao</li><br>
 * <li>创建时间：2017年5月3日 下午12:40:14</li>
 * 
 * @version 0.0.1
 */
public class FileIndexBuilder4 {

	private File dataDir;
	private File baseDir;

	private Analyzer analyzer;
	private IndexMetaDao metaDao;
	private LuceneFileDao lFileDao;

	private ExecutorService worker;
	private AtomicLong lineCount = new AtomicLong();
	private AtomicBoolean hasFlushed = new AtomicBoolean();
	private ConcurrentHashMap<File, IndexWriter> indexMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<File, BufferedWriter> writers = new ConcurrentHashMap<>();

	public static final String HOST = Utils.innerInetIp();
	public static final long EACH_FILE_SIZE = 1024 * 1024 * 200;
	public static final Charset UTF8 = Charset.forName("UTF-8");
	public static final String LINE_SPLITER = Utils.getLineSpliter();
	private static final Logger log = LoggerFactory.getLogger(FileIndexBuilder4.class);

	/***
	 * 
	 * @param srcDir
	 *            原始文件的存放目录
	 * @param indexDir
	 *            索引文件目录
	 * @param analyzer
	 *            索引分析器
	 * @param metaDao
	 *            meta信息dao
	 * @param lFileDao
	 *            Lucene文件dao
	 */
	public FileIndexBuilder4(File srcDir, File indexDir, Analyzer analyzer, IndexMetaDao metaDao, LuceneFileDao lFileDao) {
		this.metaDao = metaDao;
		this.dataDir = srcDir;
		this.lFileDao = lFileDao;
		this.analyzer = analyzer;
		this.baseDir = indexDir;
		this.worker = ThreadPools.getInstance().newPool("lucene-file-index", 1);
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
		// 日志目录:
		// --{host}:
		// ---{proc}:
		// -------log
		// ---------{-db}.log if fileSize>200 rename->-{db}.log.1
		// -------index
		// ---------lucene_index_file
		lineCount.getAndIncrement();
		String proc = ProcessorUtil.getProc(host, file);
		String fileName = file.substring(file.lastIndexOf(File.separator) + 1);
		File logFile = new File(dataDir, String.format("%s/%s/log/%s", host, proc, fileName));
		BufferedWriter writer = writers.get(logFile);
		if (writer == null) {
			synchronized (logFile) {
				if (!writers.containsKey(file)) {
					File dir = logFile.getParentFile();
					if (dir.exists() == false) {
						dir.mkdirs();
					}
					writer = Files.newBufferedWriter(logFile.toPath(), UTF8);
					writers.put(logFile, writer);
				}
			}
		}
		boolean fileChange = logFile.length() >= EACH_FILE_SIZE;
		if (fileChange) {
			synchronized (logFile) {
				writer.close();
				int seqNum = 1;
				// TODO seqNum存储到数据库
				File dir = logFile.getParentFile();
				File[] files = dir.listFiles();
				for (File item : files) {
					// name like db.log.1|db.log.2
					String name = item.getName();
					String tmp = name.substring(name.lastIndexOf(".") + 1);
					if (!Character.isDigit(tmp.charAt(0)))
						continue;
					seqNum = Math.max(seqNum, Integer.parseInt(tmp));
				}
				File newFile = new File(logFile.getAbsolutePath() + "." + seqNum);
				if (logFile.renameTo(newFile) == false) {
					throw new RuntimeException("fail to rename:" + logFile + " to:" + newFile);
				} else {
					asynAddDoc(host, newFile, logFile);
					writer = Files.newBufferedWriter(logFile.toPath(), UTF8);
					writers.put(logFile, writer);
				}
			}
		}
		writer.write(line);
		writer.write('\n');
		return fileChange;
	}

	// 异步添加document
	private void asynAddDoc(String host, File newFile, File logFile) {
		worker.execute(() -> {
			try {
				BasicFileAttributes attr = Files.readAttributes(logFile.toPath(), BasicFileAttributes.class);
				long createTime = attr.creationTime().toMillis();
				// 创建一个标志文件,索引完成后删除,以便进程异常退出时修复
				File flagFile = new File(newFile + ".wait_index");
				flagFile.createNewFile();

				Date day = Utils.toDay(createTime);
				FileReader reader = new FileReader(newFile);
				SimpleDateFormat sp = LuceneConfig.getDateFmt();

				Document doc = new Document();
				doc.add(new TextField(DocField.LINE, reader));
				doc.add(new LongPoint(DocField.TIME, day.getTime()));

				// Field.Index.NOT_ANALYZED
				doc.add(new StringField(DocField.FILE, newFile.toString(), Store.YES));
				doc.add(new StringField(DocField.SRC_FILE_NAME, logFile.getName(), Store.YES));

				File indexDir = createIndexDir(sp.format(day), host, day);
				IndexWriter writer = getIndexWriter(analyzer, indexDir);
				// 防止被flush线程关闭
				if (!writer.isOpen()) {
					writer = getIndexWriter(analyzer, indexDir);
				}
				writer.addDocument(doc);
				commitIndex(newFile.length(), flagFile);
				reader.close();
			} catch (Exception e) {
				log.error("add doc error", e);
			}
		});
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
		File indexDir = new File(baseDir, String.format("%s/%s", server, dateStr));
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
		worker.shutdown();
		writers.forEach((k, v) -> Utils.close(v));
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

	// 文件滚动时commit索引,完成后删除标记文件
	private void commitIndex(long length, File flagFile) {
		try {
			long docCount = flushIndex(indexMap);
			hasFlushed.set(true);
			boolean del = flagFile.delete();
			updateMetaInfo(length, lineCount.getAndSet(0), docCount);
			log.info("index file:{} size:{} delFlag:{}", flagFile, length, del);
		} catch (Exception e) {
			log.error(e.toString(), e);
		}
	}

}
