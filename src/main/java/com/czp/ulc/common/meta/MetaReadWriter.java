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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
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
	private static final String FILE_NAME = "f";
	private static final String LINE_NO = "l";
	private static final String LINE_SIZE = "s";
	private static final String LINE_POS = "p";

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
			writeLineIndexFile(file, zos, linePointer);
			boolean del = delSrc ? file.delete() : false;
			long end1 = System.currentTimeMillis();
			log.info("compress[{}],size[{}],delete[{}],times[{}]ms", file, file.length(), del, end1 - st1);
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

	/**
	 * 根据write返回的json信息读取文件
	 * 
	 * @param writeReturnJson
	 * @return
	 */
	public List<String> read(String writeReturnJson) {
		JSONObject json = JSONObject.parseObject(writeReturnJson);
		long lineStart = json.getLongValue(LINE_NO);
		int lineSize = json.getIntValue(LINE_SIZE);
		long linePos = json.getIntValue(LINE_POS);
		String fileStr = json.getString(FILE_NAME);
		File file = new File(fileStr);
		if (file.exists()) {
			return readFromUnCompressFile(file, linePos, lineSize);
		}
		return readFromCompressFile(file, lineStart, lineSize);
	}

	private List<String> readFromCompressFile(File file, long lineOffset, int size) {
		File zipFile = getZipFile(file);
		try (ZipFile zf = new ZipFile(zipFile)) {
			long linePointer = getLinePointer(zf, file, lineOffset);
			if (linePointer <= 0) {
				log.error("maybe index for:{} is bad", file);
				return new LinkedList<>();
			}
			ZipEntry entry = zf.getEntry(file.getName());
			return readLines(zipFile, zf.getInputStream(entry), linePointer, size, true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private LinkedList<String> readLines(File file, InputStream ins, long skip, int size, boolean closeStream)
			throws IOException {
		long st = System.currentTimeMillis();
		String tmp;
		ins.skip(skip);
		LinkedList<String> lines = new LinkedList<>();
		BufferedReader br = new BufferedReader(new InputStreamReader(ins));
		while (lines.size() < size && (tmp = br.readLine()) != null) {
			lines.add(tmp);
		}
		if (closeStream)
			br.close();
		long end = System.currentTimeMillis();
		log.info("read:[{}]lines,from:{} time:{}ms", lines.size(), file, end - st);
		return lines;
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

	private List<String> readFromUnCompressFile(File file, long offset, int size) {
		try {
			return readLines(file, new FileInputStream(file), offset, size, true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws Exception {
		Utils.close(nowWriter);
		worker.shutdown();
	}

}
