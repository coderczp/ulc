package com.czp.ulc.common.meta;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.common.kv.KVDB;
import com.czp.ulc.common.kv.LevelDB;
import com.czp.ulc.common.util.Utils;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月3日 下午12:40:14</li>
 * 
 * @version 0.0.1
 */

public class MetaReadWriter implements AutoCloseable {

	private File baseDir;
	private long eachFileSize;
	private long nowFileLines;
	private DataWriter nowWriter;
	private AtomicInteger nowFileNo = new AtomicInteger();
	private ExecutorService worker = Executors.newSingleThreadExecutor();

	/** 这些信息会存储到索引,为了节省空间,将名称简化为字符 */
	public static final String FILE_NAME = "f";
	public static final String LINE_SIZE = "s";
	public static final String LINE_NO = "l";
	public static final String LINE_POS = "p";

	private static final String SUFIX = ".log";
	private static final String ZIP_SUFIX = ".zip";
	private static final String INDEX_SUFIX = ".index";
	private static final long MAX_SKIP_BUFFER_SIZE = 1024 * 20;
	private static final long DEFAUT_EACH_FILE_SIZE = 1024 * 1024 * 200;

	private static final Logger log = LoggerFactory.getLogger(MetaReadWriter.class);

	public MetaReadWriter(String baseDir) throws Exception {
		this(baseDir, DEFAUT_EACH_FILE_SIZE);
	}

	public MetaReadWriter(String baseDir, long eachFileSize) throws Exception {
		this.baseDir = new File(baseDir);
		this.eachFileSize = eachFileSize;
		this.nowWriter = getCurrentFileWriter(this.baseDir);
		this.nowFileLines = nowWriter.getlineNo();
		this.checkHasUncompressFile();
	}

	/***
	 * 滚动写文件,返回文件的当前行号和文件号
	 * 
	 * @param lines
	 * @return
	 * @throws IOException
	 */
	public synchronized byte[] write(String line) throws Exception {
		if (nowWriter.getFile().length() >= eachFileSize) {
			Utils.close(nowWriter);
			nowWriter = getCurrentFileWriter(baseDir);
		}
		long lineNo = nowWriter.getlineNo();
		long pointer = nowWriter.getPointer();
		int size = nowWriter.writeLine(line);
		// 4字节文件ID+8字节行号
		ByteBuffer metaId = ByteBuffer.allocate(12);
		metaId.putInt(nowFileNo.get());
		metaId.putLong(lineNo);
		metaId.flip();
		byte[] array = metaId.array();
		return array;
	}

	/**
	 * 读取索引压缩文件里的行数
	 * 
	 * @return
	 */
	public long loadLineCount(File dir) {
		ZipFile zf = null;
		long count = nowFileLines;
		for (File file : dir.listFiles()) {
			if (!file.getName().endsWith(ZIP_SUFIX))
				continue;
			try {
				zf = new ZipFile(file);
				String indexFile = file.getName().replaceAll(ZIP_SUFIX, INDEX_SUFIX);
				long size = zf.getEntry(indexFile).getSize();
				count += size / Long.BYTES;
			} catch (Exception e) {
				log.error("read count error:" + file, e);
			} finally {
				Utils.close(zf);
			}
		}
		log.info("file:{} linecount:{}", dir, count);
		return count;
	}

	public void doCompress(File file, File outPut, boolean delSrc) {
		try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outPut)))) {
			long st1 = System.currentTimeMillis();
			String line = null;
			long startPos = 0, lineNo = 0;
			byte[] lineSpliter = DataWriter.lineSpliter;

			ByteBuffer metaId = ByteBuffer.allocate(12);
			ByteBuffer metaValue = ByteBuffer.allocate(12);
			Integer fileNumber = getFileNumber(file.getName());
			zos.putNextEntry(new ZipEntry(file.getName()));
			try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
				while ((line = br.readLine()) != null) {
					byte[] bytes = line.getBytes(DataWriter.UTF8);
					int len = bytes.length + lineSpliter.length;

					zos.write(bytes);
					zos.write(lineSpliter);

					metaId.putInt(fileNumber);
					metaId.putLong(lineNo);
					metaId.flip();

					metaValue.putLong(startPos);
					metaValue.putInt(len);
					metaValue.flip();

					metaDB.put(metaId.array(), metaValue.array());
					metaId.clear();
					metaValue.clear();
					lineNo++;
					startPos += len;
				}
			}
			long oldSize = file.length();
			long size = outPut.length();
			boolean del = delSrc ? file.delete() : false;
			long end1 = System.currentTimeMillis();
			log.info("compress[{}],size[{}]->[{}],del[{}],time[{}]ms", file, oldSize, size, del, end1 - st1);
		} catch (Exception e) {
			log.error("fail to compress:" + file, e);
		}
	}

	private DataWriter getCurrentFileWriter(File baseDir) throws Exception {
		int fileNo = -1;
		File nowFile = null;
		for (File item : baseDir.listFiles()) {
			if (!item.getName().endsWith(SUFIX))
				continue;
			if (item.length() >= eachFileSize) {
				ansyComprecessFile(item, true);
			} else {
				nowFile = item;
			}
			fileNo = Math.max(fileNo, getFileNumber(item.getName()));
		}
		fileNo++;
		if (nowFile == null) {
			nowFile = new File(baseDir, fileNo + SUFIX);
		}
		nowFileNo.set(fileNo);
		return new DataWriter(nowFile, true);
	}

	private void ansyComprecessFile(File file, boolean delSrc) {
		worker.execute(new Runnable() {
			@Override
			public void run() {
				doCompress(file, getZipFile(file), delSrc);
			}
		});
	}

	private static File getZipFile(File file) {
		return new File(file + ZIP_SUFIX);
	}

	private Integer getFileNumber(String name) {
		return Integer.valueOf(name.substring(0, name.indexOf(".")));
	}

	@Override
	public void close() throws Exception {
		Utils.close(nowWriter);
		worker.shutdown();
	}

	// 合并读取,避免同一个文件打开关闭多次
	public Map<Integer, Map<Long, String>> mergeRead(List<byte[]> lineRequest) throws Exception {
		Map<Integer, Map<Long, String>> datas = new HashMap<>();
		Map<Integer, Set<byte[]>> readLines = classifyByFileId(lineRequest);
		for (Entry<Integer, Set<byte[]>> entry : readLines.entrySet()) {
			Set<byte[]> jsons = entry.getValue();
			int fileId = entry.getKey();
			File file = buildFileNameWithFileId(fileId);
			long st = System.currentTimeMillis();
			datas.put(fileId, readLines(file, jsons));
			long end = System.currentTimeMillis();
			log.info("read[{}]lines,from[{}],time:[{}]ms", jsons.size(), file.getName(), end - st);
		}
		return datas;
	}

	private Map<Long, String> readLines(File file, Set<byte[]> metas) throws IOException {
		ZipFile zf = null;
		InputStream is = null;
		Map<Long, String> lineMap = new HashMap<>();
		try {
			if (file.exists()) {
				is = new FileInputStream(file);
			} else {
				zf = new ZipFile(getZipFile(file));
				ZipEntry logFile = zf.getEntry(file.getName());
				is = new BufferedInputStream(zf.getInputStream(logFile));
			}
			for (byte[] bs : metas) {
				byte[] value = metaDB.getBytes(bs);
				ByteBuffer metaId = ByteBuffer.wrap(bs);
				ByteBuffer buf = ByteBuffer.wrap(value);
				long linePos = buf.getLong();
				int size = buf.getInt();
				skipSpecBytes(file, is, linePos);
				byte[] lines = new byte[size];
				is.read(lines);
				lineMap.put(metaId.getLong(), new String(lines, DataWriter.UTF8));
			}
		} finally {
			Utils.close(is);
			Utils.close(zf);
		}
		return lineMap;
	}

	private File buildFileNameWithFileId(int fileId) {
		return new File(baseDir, fileId + ".log");
	}

	// JDK skip 不能正确的跳过指定字节,会导致CPU 100%
	private static void skipSpecBytes(File file, InputStream in, long skip) throws IOException {
		long st = System.currentTimeMillis();
		if (in instanceof FileInputStream) {
			while (skip > 0) {
				skip -= in.skip(skip);
			}
			return;
		}
		int nr = 0;
		long remaining = skip;
		int size = (int) Math.min(MAX_SKIP_BUFFER_SIZE, remaining);
		byte[] skipBuffer = new byte[size];
		while (remaining > 0 && nr < 0) {
			nr = in.read(skipBuffer, 0, (int) Math.min(size, remaining));
			remaining -= nr;
		}
		long end = System.currentTimeMillis();
		log.info("skip:[{}]bytes,from:[{}],time:[{}]ms", skip, file, end - st);
	}

	// 把读请求按文件分类,这样一个文件只打开一次
	private static Map<Integer, Set<byte[]>> classifyByFileId(List<byte[]> lineRequest) {
		Map<Integer, Set<byte[]>> readLines = new HashMap<>();
		for (byte[] meta : lineRequest) {
			ByteBuffer buf = ByteBuffer.wrap(meta);
			int metaFileNo = buf.getInt();
			Set<byte[]> lineNos = readLines.get(metaFileNo);
			if (lineNos == null) {
				lineNos = new TreeSet<>(new Comparator<byte[]>() {
					public int compare(byte[] o1, byte[] o2) {
						ByteBuffer buf1 = ByteBuffer.wrap(o1);
						ByteBuffer buf2 = ByteBuffer.wrap(o2);
						// int metaFileNo1 = buf1.getInt();
						// int metaFileNo2= buf2.getInt();
						Long metaLineNo1 = buf1.getLong();
						Long metaLineNo2 = buf2.getLong();
						return metaLineNo1.compareTo(metaLineNo2);
					}
				});
			}
			lineNos.add(meta);
			readLines.put(metaFileNo, lineNos);
		}
		return readLines;
	}

	private void checkHasUncompressFile() {
		for (File file : baseDir.listFiles()) {
			if (!file.getName().endsWith(SUFIX) || file.length() < eachFileSize)
				continue;
			ansyComprecessFile(file, true);
			log.info("find required file:{},will compress", file);
		}
	}
}
