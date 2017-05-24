package com.czp.ulc.common.meta;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.collect.handler.LogIndexHandler;
import com.czp.ulc.common.lucene.DocField;
import com.czp.ulc.common.util.Utils;

/**
 * 请添加描述 <li>创建人：Jeff.cao</li><br>
 * <li>创建时间：2017年5月3日 下午12:40:14</li>
 * 
 * @version 0.0.1
 */

public class AsynIndexManager implements AutoCloseable, FileChangeListener {

	private File dataDir;
	private File indexBaseDir;
	private LogIndexHandler handler;

	private RollingWriter writer;
	private volatile IndexMeta meta;
	private AtomicBoolean hasCompress = new AtomicBoolean();
	private ExecutorService worker = Executors.newSingleThreadExecutor();

	public static final String META_FILE_NAME = "meta.json";
	public static final String LINE_SPLITER = getLineSpliter();
	public static final Charset UTF8 = Charset.forName("UTF-8");
	private static final Logger log = LoggerFactory.getLogger(AsynIndexManager.class);

	public AsynIndexManager(File baseDir, File indexBaseDir, LogIndexHandler handler) {
		this.handler = handler;
		this.dataDir = baseDir;
		this.meta = loadMetaInfo();
		this.indexBaseDir = indexBaseDir;
		this.writer = new SyncWriter(dataDir, this);
		this.checkHasUnCompressFile(baseDir);
	}

	private static String getLineSpliter() {
		return System.getProperty("os.name").toLowerCase().contains("windows") ? "\n" : "\r";
	}

	private void checkHasUnCompressFile(File baseDir) {
		for (File file : writer.getAllFiles()) {
			if (writer.isHistoryFile(file)) {
				asynCompress(file);
			} else {
				indexUnCompressFileToRAM(file);
			}
		}
	}

	/****
	 * 滚动写文件
	 * 
	 * @param host
	 * @param file
	 * @param line
	 * @param now
	 * @return
	 * @throws Exception
	 */
	public boolean write(String host, String file, String line, long now) throws Exception {
		String string = encodeData(host, file, line, now);
		writer.append(string.getBytes(UTF8));
		return true;
	}

	private String encodeData(String host, String file, String line, long now) {
		String time = String.valueOf(now);
		StringBuilder sb = new StringBuilder(host.length() + file.length() + line.length() + time.length() + 3);
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
			log.debug("error line:{}", line);
			return null;
		}
		String time = line.substring(0, indexTime);
		String host = line.substring(indexTime + 1, indexHost);
		String srcFile = line.substring(indexHost + 1, indexFile);
		String data = line.substring(indexFile + 1, line.length());
		return new String[] { time, host, srcFile, data };
	}

	/**
	 * 进程意外退出时内存索引丢失,所以需要reload一次
	 * 
	 * @throws IOException
	 */
	private void indexUnCompressFileToRAM(File unCompressFile) {
		try (BufferedReader br = Files.newBufferedReader(unCompressFile.toPath())) {
			String line;
			long now = System.currentTimeMillis();
			while ((line = br.readLine()) != null) {
				String[] decodeData = decodeData(line);
				if (decodeData == null) {
					continue;
				}
				meta.updateRAMLines(1);
				String host = decodeData[1];
				String data = decodeData[3];
				String srcFile = decodeData[2];
				long time = Long.valueOf(decodeData[0]);
				handler.writerRAMDocument(time, srcFile, host, data);
			}
			long end = System.currentTimeMillis();
			log.info("index:{} bytes time:{} ms", unCompressFile.length(), (end - now));
		} catch (IOException e) {
			log.error("index error", e);
		}
	}

	private void asynCompress(File file) {
		Path path = file.toPath();
		worker.execute(new Runnable() {

			@Override
			public void run() {
				log.info("start index file:{}", file);
				Analyzer analyzer = handler.getAnalyzer();
				Map<File, IndexWriter> indexMap = new HashMap<>();
				Date day = Utils.igroeHMSTime(System.currentTimeMillis());
				String date = new SimpleDateFormat(LogIndexHandler.FORMAT).format(day);

				int lineCount = 0;
				long docCount = 0;
				long offset = 0;
				try (BufferedReader br = Files.newBufferedReader(path)) {
					String line;
					while ((line = br.readLine()) != null) {
						String[] decodeData = decodeData(line);
						if (decodeData == null) {
							continue;
						}
						String host = decodeData[1];
						String data = decodeData[3];
						String srcFile = decodeData[2];
						long time = Long.valueOf(decodeData[0]);

						File indexDir = createIndexDir(date, host);
						IndexWriter writer = getIndexWriter(analyzer, indexMap, indexDir);
						offset += data.length();
						lineCount++;

						Document doc = new Document();
						doc.add(new LongPoint(DocField.TIME, time));
						doc.add(new TextField(DocField.LINE, data, Field.Store.YES));
						doc.add(new TextField(DocField.FILE, srcFile, Field.Store.YES));
						writer.addDocument(doc);
					}
				} catch (Exception e) {
					log.error("fail to index:" + file, e);
				} finally {
					docCount = closeWriters(indexMap);
					boolean srcDelete = file.delete();
					hasCompress.set(true);
					handler.loadAllIndexDir();
					updateMetaInfo(offset, lineCount, docCount);
					log.info("index file:{} size:{} del:{}", file, offset, srcDelete);
				}
			}

		});
	}

	/***
	 * 更新统计信息:行数 原始字节数
	 * 
	 * @param sumBytes
	 * @param lineCount
	 * @throws IOException
	 */
	protected void updateMetaInfo(long sumBytes, long lineCount, long docCount) {
		try {
			if (sumBytes <= 0 || lineCount <= 0 || docCount <= 0) {
				log.error("error meta bytes:{} line:{} docs:{}", sumBytes, lineCount, docCount);
				return;
			}

			meta.setDocs(meta.getDocs() + docCount);
			meta.setBytes(meta.getBytes() + sumBytes);
			meta.setLines(meta.getLines() + lineCount);

			Path path = new File(dataDir.getParentFile(), META_FILE_NAME).toPath();
			Files.write(path, JSONObject.toJSONString(meta).getBytes(UTF8));

			log.info("sucess to write meta:{}", meta);
		} catch (Exception e) {
			log.error("updateMetaInfo error", e);
		}
	}

	public IndexMeta getMeta() {
		return meta;
	}

	/***
	 * 加载meta信息
	 * 
	 * @return
	 */
	private IndexMeta loadMetaInfo() {
		try {
			File metaFile = new File(dataDir.getParentFile(), META_FILE_NAME);
			if (!metaFile.exists() || metaFile.length() == 0)
				return IndexMeta.EMPTY;

			byte[] bytes = Files.readAllBytes(metaFile.toPath());
			return JSONObject.parseObject(new String(bytes), IndexMeta.class);
		} catch (IOException e) {
			log.error("loadMeta index", e);
		}
		return IndexMeta.EMPTY;

	}

	public long getFolderBytes(Path path) throws IOException {
		return Files.walk(path).map(f -> f.toFile()).filter(f -> f.isFile()).mapToLong(f -> f.length()).sum();
	}

	/***
	 * 根据日期创建索引目录
	 * 
	 * @param date
	 * @param host
	 * @return
	 */
	protected File createIndexDir(String date, String host) {
		File indexDir = new File(indexBaseDir, String.format("%s/%s", host, date));
		indexDir.mkdirs();
		return indexDir;
	}

	protected long closeWriters(Map<File, IndexWriter> indexMap) {
		long count = 0;
		for (Entry<File, IndexWriter> entry : indexMap.entrySet()) {
			IndexWriter value = entry.getValue();
			try {
				value.commit();
				count += value.numDocs();
				value.close();
			} catch (Exception e) {
				log.error("fail to close index writer:" + value, e);
			}
		}
		return count;
	}

	protected IndexWriter getIndexWriter(Analyzer analyzer, Map<File, IndexWriter> indexMap, File indexDir) {
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

	@Override
	public void close() throws Exception {
		writer.close();
		worker.shutdown();
	}

	/**
	 * 标记上一个文件是否压缩完成
	 * 
	 * @param b
	 * @return
	 */
	public boolean checkHasCompress() {
		return hasCompress.getAndSet(false);
	}

	@Override
	public void onFileChange(File currentFile) {
		asynCompress(currentFile);
	}

}
