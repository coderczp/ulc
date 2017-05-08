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

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.common.util.Utils;

/**
 * 请添加描述 <li>创建人：Jeff.cao</li> <li>创建时间：2017年5月3日 下午12:40:14</li>
 * 
 * @version 0.0.1
 */

public class MetaReadWriter implements AutoCloseable {

	private File baseDir;
	private long eachFileSize;
	private long nowFileLines;
	private DataWriter nowWriter;
	private ExecutorService worker = Executors.newSingleThreadExecutor();

	/** 这些信息会存储到索引,为了节省空间,将名称简化为字符 */
	public static final String FILE_NAME = "f";
	public static final String LINE_SIZE = "s";
	public static final String LINE_NO = "l";

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
		this.nowWriter = getCurrentFileWriter(getTodayDir(this.baseDir));
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
	public synchronized String write(String line) throws Exception {
		if (nowWriter.getFile().length() >= eachFileSize) {
			Utils.close(nowWriter);
			nowWriter = getCurrentFileWriter(getTodayDir(baseDir));
		}
		long lineNo = nowWriter.getlineNo();
		long pointer = nowWriter.getPointer();
		nowWriter.writeLine(line);
		nowWriter.writeIndex(lineNo, pointer);
		JSONObject json = new JSONObject();
		json.put(LINE_NO, lineNo);
		json.put(FILE_NAME, nowWriter.getFile());
		return json.toJSONString();
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
			if (!file.isDirectory())
				continue;
			for (File item : file.listFiles()) {
				if (!item.getName().endsWith(ZIP_SUFIX) || item.length() == 0)
					continue;
				try {
					zf = new ZipFile(item);
					String indexFile = item.getName().replaceAll(ZIP_SUFIX, INDEX_SUFIX);
					long size = zf.getEntry(indexFile).getSize();
					count += size / Long.BYTES;
				} catch (Exception e) {
					log.error("read count error:" + item, e);
				} finally {
					Utils.close(zf);
				}
			}
		}
		log.info("file:{} linecount:{}", dir, count);
		return count;
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
			long oldSize = file.length();
			long size = outPut.length();
			writeLineIndexFile(file, zos, linePointer);
			if (delSrc) {
				del = file.delete();
				File indexFile = new File(file.getParentFile(), getIndexFileName(file));
				if (indexFile.exists())
					indexFile.delete();
			}
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
		zos.putNextEntry(new ZipEntry(getIndexFileName(file)));
		for (Long num : index) {
			zos.write(Utils.longToBytes(num));
		}
	}

	public static String getIndexFileName(File file) {
		return file.getName().concat(INDEX_SUFIX);
	}

	private static File getZipFile(File file) {
		return new File(file + ZIP_SUFIX);
	}

	private Integer getFileNumber(String name) {
		return Integer.valueOf(name.substring(0, name.indexOf(".")));
	}

	private File getTodayDir(File baseDir) {
		Date day = Utils.igroeHMSTime(System.currentTimeMillis());
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		String fileStr = format.format(day);
		File file = new File(baseDir, fileStr);
		file.mkdirs();
		return file;
	}

	private static long[] getCompressFileLinePos(ZipFile zf, File file, long lineNo) throws IOException {
		ZipEntry indexFile = zf.getEntry(getIndexFileName(file));
		return readLinePos(file, zf.getInputStream(indexFile), lineNo);
	}

	// 先读取索引,在根据索引快速定位行对应的文件指针读取行
	private static long[] readLinePos(File file, InputStream is, long lineNo) throws IOException {
		try (InputStream io = is) {
			if (lineNo < 0) {
				return EMPTY_LONG_ARR;
			}

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

	public static Map<Long, String> readFromUnCompressFile(File file, InputStream is, Set<JSONObject> lineRequest)
			throws IOException {
		// 纪录已经跳过的字节数
		long lastSkip = 0;
		// 纪录已经读取的字节数
		long hasReadSize = 0;
		// 映射行号和内容
		Map<Long, String> lineMap = new HashMap<>();
		try (InputStream ins = is) {
			String indexPath = getIndexFileName(file);
			File indexFile = new File(file.getParentFile(), indexPath);
			for (JSONObject json : lineRequest) {
				FileInputStream fis = new FileInputStream(indexFile);
				long lineNo = json.getLongValue(LINE_NO);
				long[] linePos = readLinePos(file, fis, lineNo);
				int lineBytes = (int) linePos[1];
				if (linePos[0] < 0 || lineBytes <= 0) {
					log.error("can't find index for line:{} in:{}", lineNo, file);
					lineMap.put(lineNo, "N/A");
				} else {
					long skip = linePos[0] - lastSkip - hasReadSize;
					skipSpecBytes(file, is, skip);
					byte[] buf = new byte[lineBytes];
					is.read(buf);
					lineMap.put(lineNo, new String(buf, DataWriter.UTF8));
					hasReadSize += buf.length;
					lastSkip = skip;
				}
			}
		}
		return lineMap;
	}

	// 合并读取,避免同一个文件打开关闭多次
	public static Map<String, Map<Long, String>> mergeRead(List<JSONObject> lineRequest) throws Exception {
		Map<String, Map<Long, String>> datas = new HashMap<>();
		Map<String, Set<JSONObject>> readLines = classifyByFile(lineRequest);
		for (Entry<String, Set<JSONObject>> entry : readLines.entrySet()) {
			Set<JSONObject> jsons = entry.getValue();
			String fileName = entry.getKey();
			File file = new File(fileName);
			Map<Long, String> lines = null;
			String logName = fileName;

			long st = System.currentTimeMillis();
			if (file.exists()) {
				lines = readFromUnCompressFile(file, new FileInputStream(file), jsons);
			} else {
				lines = readFromCompressFile(jsons, file);
				logName = getZipFile(file).getName();
			}
			datas.put(fileName, lines);
			long end = System.currentTimeMillis();
			log.info("read[{}]lines,from[{}],time:[{}]ms", jsons.size(), logName, end - st);
		}
		return datas;
	}

	private static Map<Long, String> readFromCompressFile(Set<JSONObject> jsons, File file) throws Exception {
		File zipFile = getZipFile(file);
		Map<Long, String> linesMap = new HashMap<>();
		try (ZipFile zf = new ZipFile(zipFile)) {
			ZipEntry logFile = zf.getEntry(file.getName());
			try (InputStream br = zf.getInputStream(logFile)) {
				long lastSkip = 0, hasReadSize = 0;
				for (JSONObject json : jsons) {
					System.out.println(json);
					long lineNo = json.getLongValue(LINE_NO);
					long[] linePointer = getCompressFileLinePos(zf, file, lineNo);
					int lineBytes = (int) linePointer[1];
					if (linePointer[0] < 0 || lineBytes <= 0) {
						log.error("can't find index for line:{} in:{}", lineNo, zipFile);
						linesMap.put(lineNo, "N/A");
					} else {
						long skip = linePointer[0] - lastSkip - hasReadSize;
						skipSpecBytes(zipFile, br, skip);
						byte[] buf = new byte[lineBytes];
						br.read(buf);
						linesMap.put(lineNo, new String(buf, DataWriter.UTF8));
						hasReadSize += buf.length;
						lastSkip = skip;
					}
				}
			}
		}
		return linesMap;
	}

	// JDK skip 不能正确的跳过指定字节,会导致CPU 100%
	private static void skipSpecBytes(File zipFile, InputStream in, long skip) throws IOException {
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
		log.info("skip:[{}]bytes,from:[{}],time:[{}]ms", skip, zipFile, end - st);
	}

	// 把读请求按文件分类,这样一个文件只打开一次
	private static Map<String, Set<JSONObject>> classifyByFile(List<JSONObject> lineRequest) {
		Map<String, Set<JSONObject>> readLines = new HashMap<>();
		for (JSONObject json : lineRequest) {
			String fileName = (String) json.remove(FILE_NAME);
			Set<JSONObject> lineNos = readLines.get(fileName);
			if (lineNos != null) {
				lineNos.add(json);
				continue;
			}
			lineNos = new TreeSet<>(new Comparator<JSONObject>() {
				public int compare(JSONObject o1, JSONObject o2) {
					return o1.getInteger(LINE_NO).compareTo(o2.getInteger(LINE_NO));
				}
			});
			lineNos.add(json);
			readLines.put(fileName, lineNos);
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
