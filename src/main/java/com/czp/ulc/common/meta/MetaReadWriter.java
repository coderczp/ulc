package com.czp.ulc.common.meta;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.WeakHashMap;
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

	private String baseDir;
	private long eachFileSize;
	private DataWriter nowWriter;
	private Logger log = LoggerFactory.getLogger(MetaReadWriter.class);
	private ExecutorService worker = Executors.newSingleThreadExecutor();
	private WeakHashMap<File, WeakHashMap<Long, Long>> indexMap = new WeakHashMap<>();

	/** 这些信息会存储到索引,为了节省空间,将名称简化为字符 */
	public static final String FILE_NAME = "f";
	public static final String LINE_NO = "l";
	public static final String LINE_SIZE = "s";
	public static final String LINE_POS = "p";

	private static final String SUFIX = ".log";
	private static final String ZIP_SUFIX = ".zip";
	private static final String INDEX_SUFIX = ".index";

	public MetaReadWriter(String baseDir, long eachFileSize) throws Exception {
		this.baseDir = baseDir;
		this.eachFileSize = eachFileSize;
		this.nowWriter = getCurrentFileWriter(getTodayDir(baseDir));
	}

	public MetaReadWriter(String baseDir) throws Exception {
		this(baseDir, 1024 * 1024 * 200);
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

	private void doCompress(File file, File outPut, boolean delSrc) {
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outPut))) {
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
			long length = file.length();
			writeLineIndexFile(file, zos, linePointer);
			boolean del = delSrc ? file.delete() : false;
			long end1 = System.currentTimeMillis();
			log.info("compress[{}],size[{}],delete[{}],times[{}]ms", file, length, del, end1 - st1);
		} catch (Exception e) {
			log.error("fail to compress:" + file, e);
		}
	}

	private void writeLineIndexFile(File file, ZipOutputStream zos, LinkedList<Long> index) throws IOException {
		zos.putNextEntry(new ZipEntry(getIndexFileName(file)));
		for (Long num : index) {
			zos.write(Utils.longToBytes(num));
		}
	}

	private String getIndexFileName(File file) {
		return file.getName().concat(INDEX_SUFIX);
	}

	private File getZipFile(File file) {
		return new File(file + ZIP_SUFIX);
	}

	/***
	 * 滚动写文件,返回文件的当前行号
	 * 
	 * @param lines
	 * @return
	 * @throws IOException
	 */
	public synchronized String write(List<String> lines) throws Exception {

		if (nowWriter.getFile().length() >= eachFileSize) {
			Utils.close(nowWriter);
			nowWriter = getCurrentFileWriter(getTodayDir(baseDir));
		}

		int lineSize = 0;
		long lineNo = nowWriter.getlineNo();
		long pointer = nowWriter.getPointer();
		for (String string : lines) {
			String line = string.trim();
			if (line.length() > 0) {
				nowWriter.writeLine(string);
				lineSize++;
			}
		}
		JSONObject json = new JSONObject();
		json.put(LINE_NO, lineNo);
		json.put(LINE_POS, pointer);
		json.put(LINE_SIZE, lineSize);
		json.put(FILE_NAME, nowWriter.getFile());
		return json.toJSONString();
	}

	private Integer getFileNumber(String name) {
		return Integer.valueOf(name.substring(0, name.indexOf(".")));
	}

	private File getTodayDir(String baseDir) {
		Date day = Utils.igroeHMSTime(System.currentTimeMillis());
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		String fileStr = format.format(day);
		File file = new File(baseDir, fileStr);
		file.mkdirs();
		return file;
	}

	// 先读取索引,在根据索引快速定位行对应的文件指针读取行
	private long getLinePointer(ZipFile zf, File file, long lineNo) throws IOException {
		if (lineNo == 0)
			return 0;
		WeakHashMap<Long, Long> lineIndexMap = indexMap.get(file);
		if (lineIndexMap != null) {
			return lineIndexMap.getOrDefault(lineNo, -1l);
		}
		synchronized (file) {
			if (!indexMap.containsKey(file))
				loadAllLineIndex(zf, file);
		}
		return indexMap.get(file).getOrDefault(lineNo, -1l);
	}

	private void loadAllLineIndex(ZipFile zf, File file) {
		long line = 0;
		byte[] bytes = new byte[8];
		ZipEntry indexFile = zf.getEntry(getIndexFileName(file));
		WeakHashMap<Long, Long> lineIndexMap = new WeakHashMap<>();
		try (BufferedInputStream dis = new BufferedInputStream(zf.getInputStream(indexFile))) {
			while (dis.read(bytes) != -1) {
				lineIndexMap.put(line++, Utils.bytesToLong(bytes));
			}
		} catch (Throwable e) {
			log.error("load line index error:" + file, e);
		}
		if (lineIndexMap.size() > 0)
			indexMap.put(file, lineIndexMap);
	}

	@Override
	public void close() throws Exception {
		Utils.close(nowWriter);
		worker.shutdown();
	}

	public Map<Long, String> readFromUnCompressFile(InputStream is, Set<JSONObject> lineRequest) throws IOException {
		// 纪录已经跳过的字节数
		long lastSkip = 0;
		// 纪录已经读取的字节数
		long hasReadSize = 0;
		// 映射行号和内容
		Map<Long, String> lineMap = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			for (JSONObject json : lineRequest) {
				int size = json.getIntValue(LINE_SIZE);
				long lineNo = json.getLongValue(LINE_NO);
				long linePos = json.getLongValue(LINE_POS);
				long skip = linePos - lastSkip - hasReadSize;
				if (skip > 0)
					is.skip(skip);

				String line;
				StringBuilder sb = new StringBuilder();
				while (size-- > 0 && (line = br.readLine()) != null) {
					sb.append(line).append(DataWriter.LINE_SPLITER);
					hasReadSize += line.length();
				}
				lineMap.put(lineNo, sb.toString());
				lastSkip = linePos;
			}
		}
		return lineMap;
	}

	// 合并读取,避免同一个文件打开关闭多次
	public Map<String, Map<Long, String>> mergeRead(List<JSONObject> lineRequest) throws Exception {
		Map<String, Map<Long, String>> datas = new HashMap<>();
		Map<String, Set<JSONObject>> readLines = classifyByFile(lineRequest);
		for (Entry<String, Set<JSONObject>> entry : readLines.entrySet()) {
			Set<JSONObject> jsons = entry.getValue();
			String fileName = entry.getKey();
			File file = new File(fileName);
			Map<Long, String> lines = null;
			long st = System.currentTimeMillis();
			String logName = fileName;
			if (file.exists()) {
				lines = readFromUnCompressFile(new FileInputStream(file), jsons);
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

	private Map<Long, String> readFromCompressFile(Set<JSONObject> jsons, File file) throws Exception {
		File zipFile = getZipFile(file);
		Map<Long, String> linesMap = new HashMap<>();
		try (ZipFile zf = new ZipFile(zipFile)) {
			ZipEntry zentry = zf.getEntry(file.getName());
			InputStream is = zf.getInputStream(zentry);
			long lastSkip = 0;
			long hasReadSize = 0;
			try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
				for (JSONObject json : jsons) {
					int size = json.getIntValue(LINE_SIZE);
					long lineNo = json.getLongValue(LINE_NO);
					long linePointer = getLinePointer(zf, file, lineNo);
					if (linePointer <= 0) {
						log.error("can't find index for line:{} in:{}", lineNo, zipFile);
						linesMap.put(lineNo, "N/A");
					} else {
						String line;
						long st = System.currentTimeMillis();
						long skip = linePointer - lastSkip - hasReadSize;
						if (skip > 0)
							is.skip(skip);
						long end = System.currentTimeMillis();
						log.info("skip:{}bytes from:{} time:{}", skip, zipFile, end - st);
						StringBuilder sb = new StringBuilder();
						while (size-- > 0 && (line = br.readLine()) != null) {
							sb.append(line).append(DataWriter.LINE_SPLITER);
							hasReadSize += line.length();
						}
						linesMap.put(lineNo, sb.toString());
						lastSkip = lineNo;
					}
				}
			}
		}
		return linesMap;
	}

	// 把读请求按文件分类,这样一个文件只打开一次
	private Map<String, Set<JSONObject>> classifyByFile(List<JSONObject> lineRequest) {
		Map<String, Set<JSONObject>> readLines = new HashMap<>();
		for (JSONObject json : lineRequest) {
			String fileName = (String) json.remove(FILE_NAME);
			Set<JSONObject> lineNos = readLines.get(fileName);
			if (lineNos == null) {
				lineNos = new TreeSet<>(new Comparator<JSONObject>() {
					public int compare(JSONObject o1, JSONObject o2) {
						return o1.getInteger(LINE_NO).compareTo(o2.getInteger(LINE_NO));
					}
				});
				readLines.put(fileName, lineNos);
			}
			lineNos.add(json);
		}
		return readLines;
	}
}
