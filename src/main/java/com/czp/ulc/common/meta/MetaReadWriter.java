package com.czp.ulc.common.meta;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private ExecutorService worker = Executors.newSingleThreadExecutor();

	private static final String SUFIX = ".log";
	private static final String ZIP_SUFIX = ".zip";
	private static final String INDEX_SUFIX = ".index";
	private static final long MAX_SKIP_BUFFER_SIZE = 1024 * 20;
	private static final long DEFAUT_EACH_FILE_SIZE = 1024 * 1024 * 200;

	private static final long[] EMPTY_LONG_ARR = new long[] { 0, 0 };
	private static final Logger log = LoggerFactory.getLogger(MetaReadWriter.class);

	public MetaReadWriter(String baseDir) throws Exception {
		this(baseDir, DEFAUT_EACH_FILE_SIZE);
	}

	public MetaReadWriter(String baseDir, long eachFileSize) throws Exception {
		this.baseDir = new File(baseDir);
		this.eachFileSize = eachFileSize;
		this.nowWriter = getTodayWriter(getTodayDir(this.baseDir));
		this.nowFileLines = nowWriter.getlineNo();
		this.checkHasUncompressFile();
	}

	/***
	 * 滚动写文件,返回文件的当前行号
	 * 
	 * @param lines
	 * @return
	 * @throws IOException
	 */
	public synchronized byte[] write(String line) throws Exception {
		if (nowWriter.getFile().length() >= eachFileSize) {
			Utils.close(nowWriter);
			nowWriter = getTodayWriter(getTodayDir(baseDir));
		}
		int dirId = nowWriter.getDirId();
		int fileId = nowWriter.getFileId();
		long lineNo = nowWriter.getlineNo();
		long pointer = nowWriter.getPointer();
		nowWriter.writeLine(line);
		nowWriter.writeIndex(lineNo, pointer);
		return encodeMetaId(dirId, fileId, lineNo);
	}

	/***
	 * 4字节目录编号 4字节文件编号 8字节行号将文件id和目录ID编码为一个long
	 * 
	 * @param dirId
	 * @param fileId
	 * @param lineNo
	 * @return
	 */
	public static byte[] encodeMetaId(int dirId, int fileId, long lineNo) {
		ByteBuffer buf = ByteBuffer.allocate(16);
		long uuId = ((long) dirId << 32) | fileId;
		buf.putLong(uuId);
		buf.putLong(lineNo);
		return buf.array();
	}

	public static long[] decodeMetaId(byte[] metaId) {
		ByteBuffer buf = ByteBuffer.wrap(metaId);
		long uuid = buf.getLong();
		long lineNo = buf.getLong();
		return new long[] { uuid, lineNo };
	}

	private String getFileNameFrom(long uuid) {
		int dirId = (int) (uuid >> 32), fileId = (int) uuid;
		return String.format("%s/%s/%s%s", baseDir, dirId, fileId, SUFIX);
	}

	/**
	 * 读取索引压缩文件里的行数
	 * 
	 * @return
	 */
	public long loadLineCount(File dir) {
		long bytes = 0;
		for (File file : dir.listFiles()) {
			if (!file.isDirectory())
				continue;
			for (File item : file.listFiles()) {
				if (!item.getName().endsWith(ZIP_SUFIX) || item.length() == 0)
					continue;
				try (ZipFile zf = new ZipFile(item)) {
					String indexFile = item.getName().replaceAll(ZIP_SUFIX, INDEX_SUFIX);
					bytes += zf.getEntry(indexFile).getSize();
				} catch (Exception e) {
					log.error("read count error:" + item, e);
				}
			}
		}
		long lineCount = nowFileLines + (bytes / Long.BYTES);
		log.info("file:{} linecount:{}", dir, lineCount);
		return lineCount;

	}

	public static void doCompress(File file, File outPut, boolean delSrc) {
		try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outPut)))) {
			long st1 = System.currentTimeMillis();
			long startPos = 0;
			String line = null;
			byte[] lineSpliter = DataWriter.lineSpliter;
			zos.putNextEntry(new ZipEntry(file.getName()));
			LinkedList<Long> linePointer = new LinkedList<>();
			try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
				while ((line = br.readLine()) != null) {
					byte[] bytes = line.getBytes(DataWriter.UTF8);
					startPos += bytes.length + lineSpliter.length;
					linePointer.add(startPos);
					zos.write(bytes);
					zos.write(lineSpliter);
				}
			}
			boolean del = false;
			long size = outPut.length();
			long oldSize = file.length();
			writeLineIndexFile(file, zos, linePointer);
			if (delSrc) {
				del = file.delete();
				File indexFile = getIndexFile(file);
				if (indexFile.exists())
					indexFile.delete();
			}
			long end1 = System.currentTimeMillis();
			log.info("compress[{}],size[{}]->[{}],del[{}],time[{}]ms", file, oldSize, size, del, end1 - st1);
		} catch (Exception e) {
			log.error("fail to compress:" + file, e);
		}
	}

	private DataWriter getTodayWriter(File baseDir) throws Exception {
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
			fileNo = Math.max(fileNo, getFileId(item));
		}
		fileNo++;
		if (nowFile == null) {
			nowFile = new File(baseDir, fileNo + SUFIX);
		}
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

	private static void writeLineIndexFile(File file, ZipOutputStream zos, LinkedList<Long> index) throws IOException {
		zos.putNextEntry(new ZipEntry(getIndexFile(file).getName()));
		for (Long num : index) {
			zos.write(Utils.longToBytes(num));
		}
	}

	public static File getIndexFile(File file) {
		return new File(file + INDEX_SUFIX);
	}

	private static File getZipFile(File file) {
		return new File(file + ZIP_SUFIX);
	}

	public static int getFileId(File file) {
		String name = file.getName();
		return Integer.parseInt(name.substring(0, name.indexOf(".")));
	}

	private File getTodayDir(File baseDir) {
		Date day = Utils.igroeHMSTime(System.currentTimeMillis());
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		String fileStr = format.format(day);
		File file = new File(baseDir, fileStr);
		file.mkdirs();
		return file;
	}

	private long[] getLinePos(ZipFile zf, File file, long lineNo) throws IOException {
		if (zf == null) {
			return readLinePos(file, new FileInputStream(getIndexFile(file)), lineNo);
		}
		ZipEntry indexFile = zf.getEntry(getIndexFile(file).getName());
		return readLinePos(file, zf.getInputStream(indexFile), lineNo);
	}

	// 先读取索引,在根据索引快速定位行对应的文件指针读取行
	private long[] readLinePos(File file, InputStream is, long lineNo) throws IOException {
		try (InputStream io = is) {
			if (lineNo < 0)
				return EMPTY_LONG_ARR;
			int bytes = Long.BYTES;
			long skip = lineNo * bytes;
			byte[] buf = new byte[bytes * 2];
			skipSpecBytes(file, is, skip);
			is.read(buf);
			ByteBuffer wrap = ByteBuffer.wrap(buf);
			long fristLineOffset = wrap.getLong();
			long lastLineOffset = wrap.getLong();
			return new long[] { fristLineOffset, lastLineOffset - fristLineOffset };
		}
	}

	@Override
	public void close() throws Exception {
		Utils.close(nowWriter);
		worker.shutdown();
	}

	public Map<Long, String> readFromLogFile(File file, InputStream is, Set<byte[]> lineRequest) throws IOException {
		return readMetaData(lineRequest, file, is, null);
	}

	// 合并读取,避免同一个文件打开关闭多次
	public Map<Long, Map<Long, String>> mergeRead(List<byte[]> lineRequest) throws Exception {
		Map<Long, Map<Long, String>> datas = new HashMap<>();
		Map<Long, Set<byte[]>> readLines = classifyByFile(lineRequest);
		for (Entry<Long, Set<byte[]>> entry : readLines.entrySet()) {
			long st = System.currentTimeMillis();
			Set<byte[]> jsons = entry.getValue();
			long uuid = entry.getKey();

			String fileName = getFileNameFrom(uuid);
			File file = new File(fileName);
			Map<Long, String> lines = null;
			String logName = fileName;
			if (file.exists()) {
				lines = readFromLogFile(file, new FileInputStream(file), jsons);
			} else {
				lines = readFromZipFile(jsons, file);
				logName = getZipFile(file).getName();
			}
			datas.put(uuid, lines);
			long end = System.currentTimeMillis();
			log.info("read[{}]lines,from[{}],time:[{}]ms", jsons.size(), logName, end - st);
		}
		return datas;
	}

	private Map<Long, String> readFromZipFile(Set<byte[]> lineRequest, File file) throws Exception {
		try (ZipFile zf = new ZipFile(getZipFile(file))) {
			ZipEntry logFile = zf.getEntry(file.getName());
			return readMetaData(lineRequest, file, zf.getInputStream(logFile), zf);
		}
	}

	private Map<Long, String> readMetaData(Set<byte[]> metas, File file, InputStream ins, ZipFile zf)
			throws IOException {
		Map<Long, String> linesMap = new HashMap<>();
		try (InputStream br = ins) {
			long lastSkip = 0, hasReadSize = 0;
			for (byte[] item : metas) {
				long[] meta = decodeMetaId(item);
				long lineNo = meta[1];
				long[] linePointer = getLinePos(zf, file, lineNo);
				int size = (int) linePointer[1];
				long pos = linePointer[0];
				if (pos < 0 || size <= 0) {
					log.error("find index err,file:{},line:{},pos:{},size:{}", file, lineNo, pos, size);
					linesMap.put(lineNo, "N/A");
				} else {
					long skip = pos - lastSkip - hasReadSize;
					skipSpecBytes(file, br, skip);
					byte[] buf = new byte[size];
					br.read(buf);
					linesMap.put(lineNo, new String(buf, DataWriter.UTF8));
					hasReadSize += buf.length;
					lastSkip = skip;
				}
			}
		}
		return linesMap;
	}

	// JDK skip 不能正确的跳过指定字节,会导致CPU 100%
	private static void skipSpecBytes(File file, InputStream in, long skip) throws IOException {
		if (skip <= 0)
			return;
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
	private Map<Long, Set<byte[]>> classifyByFile(List<byte[]> lineRequest) {
		Map<Long, Set<byte[]>> readLines = new HashMap<>();
		for (byte[] item : lineRequest) {
			long[] meta = decodeMetaId(item);
			long uuid = meta[0];
			Set<byte[]> lineNos = readLines.get(uuid);
			if (lineNos == null) {
				lineNos = new TreeSet<>(new Comparator<byte[]>() {
					public int compare(byte[] o1, byte[] o2) {
						Long lineId = decodeMetaId(o1)[1];
						Long lineId2 = decodeMetaId(o2)[1];
						return lineId.compareTo(lineId2);
					}
				});
			}
			lineNos.add(item);
			readLines.put(uuid, lineNos);
		}
		return readLines;
	}

	private void checkHasUncompressFile() {
		File tadayFile = getTodayDir(baseDir);
		for (File file : baseDir.listFiles()) {
			if (file.getName().equals(tadayFile.getName()))
				continue;
			for (File item : file.listFiles()) {
				if (!item.getName().endsWith(SUFIX))
					continue;
				ansyComprecessFile(item, true);
				log.info("find required file:{},will compress", item);
			}
		}
	}
}
