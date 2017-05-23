package com.czp.ulc.common.meta;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.collect.handler.DocField;
import com.czp.ulc.collect.handler.LuceneLogHandler;
import com.czp.ulc.common.util.Utils;

/**
 * 请添加描述 <li>创建人：Jeff.cao</li><br>
 * <li>创建时间：2017年5月3日 下午12:40:14</li>
 * 
 * @version 0.0.1
 */

public class CompressManager implements AutoCloseable, FileChangeListener {

	private File zipDir;
	private File dataDir;
	private File buffDir;
	private File indexBaseDir;
	private LuceneLogHandler handler;

	private RollingWriter writer;
	private volatile CompressMeta meta;
	private AtomicBoolean hasCompress = new AtomicBoolean();
	private ExecutorService worker = Executors.newSingleThreadExecutor();

	public static final String SUFFIX = ".gz";
	public static final String META_FILE_NAME = "meta.json";
	public static final String LINE_SPLITER = getLineSpliter();
	public static final Charset UTF8 = Charset.forName("UTF-8");
	public static byte[] SPLITER_BYTES = LINE_SPLITER.getBytes(UTF8);
	private static final Logger log = LoggerFactory.getLogger(CompressManager.class);

	public CompressManager(File baseDir, File zipDir, File indexBaseDir, LuceneLogHandler handler) {
		this.zipDir = zipDir;
		this.handler = handler;
		this.dataDir = baseDir;
		this.meta = loadMetaInfo();
		this.indexBaseDir = indexBaseDir;
		this.writer = new SyncWriter(dataDir, this);
		this.checkHasUnCompressFile(baseDir);
		this.buffDir = new File(zipDir.getParentFile(), "cache");
		this.buffDir.mkdirs();
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
		log.error("start compress file:{}", file);
		Path path = file.toPath();
		worker.execute(new Runnable() {

			@Override
			public void run() {
				Analyzer analyzer = handler.getAnalyzer();
				Map<File, IndexWriter> indexMap = new HashMap<>();
				Date day = Utils.igroeHMSTime(System.currentTimeMillis());
				String date = new SimpleDateFormat(LuceneLogHandler.FORMAT).format(day);

				int offset = 0;
				long docCount = 0;
				int lineCount = 0;
				GZIPOutputStream gzos = null;
				try (BufferedReader br = Files.newBufferedReader(path)) {
					String line;
					File zipFile = getCompressFile(zipDir);
					int fileId = Utils.getFileId(zipFile);
					gzos = getOutputStream(zipFile);
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
						byte[] bs = data.getBytes(UTF8);
						gzos.write(bs);
						gzos.write(SPLITER_BYTES);

						int allSize = bs.length + SPLITER_BYTES.length;
						offset += allSize;
						lineCount++;

						Document doc = new Document();
						doc.add(new LongPoint(DocField.TIME, time));
						doc.add(new StoredField(DocField.OFFSET, offset));
						doc.add(new StoredField(DocField.META_FILE, fileId));
						doc.add(new TextField(DocField.LINE, data, Field.Store.NO));
						doc.add(new TextField(DocField.FILE, srcFile, Field.Store.YES));
						writer.addDocument(doc);

					}
				} catch (Exception e) {
					log.error("fail to index:" + file, e);
				} finally {
					Utils.close(gzos);
					boolean delete = file.delete();
					docCount = closeWriters(indexMap);

					hasCompress.set(true);
					handler.loadAllIndexDir();
					updateMetaInfo(offset, lineCount, docCount);
					log.info("compress file:{} size:{} del:{}", file, offset, delete);
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
				log.error("error meta bytes:{} line:{} dco:{}", sumBytes, lineCount, docCount);
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

	public CompressMeta getMeta() {
		return meta;
	}

	/***
	 * 加载meta信息
	 * 
	 * @return
	 */
	private CompressMeta loadMetaInfo() {
		try {
			File metaFile = new File(dataDir.getParentFile(), META_FILE_NAME);
			if (!metaFile.exists() || metaFile.length() == 0)
				return CompressMeta.EMPTY;

			byte[] bytes = Files.readAllBytes(metaFile.toPath());
			JSONObject meta = JSONObject.parseObject(new String(bytes));
			return JSONObject.toJavaObject(meta, CompressMeta.class);
		} catch (IOException e) {
			log.error("loadMeta index", e);
		}
		return CompressMeta.EMPTY;

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

	/***
	 * 关闭压缩文件流
	 * 
	 * @param zosMap
	 */
	protected void closeAllOutputStream(Map<File, GZIPOutputStream> zosMap) {
		zosMap.values().forEach(item -> Utils.close(item));
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

	protected GZIPOutputStream getOutputStream(File zipFile) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(zipFile));
		return new GZIPOutputStream(out);
	}

	protected File getCompressFile(File zipDir) {
		int fileId = -1;
		for (File file : zipDir.listFiles()) {
			fileId = Math.max(fileId, Utils.getFileId(file));
		}
		return createGzipFile(fileId + 1, zipDir);
	}

	public synchronized String readLine(int fileId, int offset) {
		long st = System.currentTimeMillis();
		File logFile = new File(String.format("%s/%s%s", buffDir, fileId, ".log"));
		if (!logFile.exists()) {
			deCompressFile(fileId, logFile);
		}
		try (FileInputStream is = new FileInputStream(logFile)) {
			is.skip(offset);
			BufferedReader br = new BufferedReader(new InputStreamReader(is, UTF8));
			String line = br.readLine();
			br.close();
			long end = System.currentTimeMillis();
			log.debug("load data time:{} ms", (end - st));
			return line;
		} catch (Exception e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	private void deCompressFile(int fileId, File logFile) {
		File zipFile = new File(String.format("%s/%s%s", zipDir, fileId, SUFFIX));
		try (GZIPInputStream is = new GZIPInputStream(new FileInputStream(zipFile))) {
			try (FileOutputStream fis = new FileOutputStream(logFile)) {
				byte[] buf = new byte[1024 * 8];
				int n = -1;
				while ((n = is.read(buf)) != -1) {
					fis.write(buf, 0, n);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	private File createGzipFile(int fileId, File baseDir) {
		return new File(String.format("%s/%s%s", baseDir, fileId, SUFFIX));
	}

	private IndexWriter createIndexWriter(Analyzer analyzer, File file) {
		try {
			TieredMergePolicy mergePolicy = new TieredMergePolicy();
			IndexWriterConfig conf = new IndexWriterConfig(analyzer);
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
