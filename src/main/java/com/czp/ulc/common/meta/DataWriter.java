package com.czp.ulc.common.meta;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月4日 上午11:27:03</li>
 * 
 * @version 0.0.1
 */

public class DataWriter implements AutoCloseable {

	private File file;
	private int dirId;
	private int fileId;
	private long lineNo;
	private long pointer;
	private BufferedOutputStream writer;
	private AtomicBoolean change = new AtomicBoolean();

	public static Charset UTF8 = Charset.forName("UTF-8");

	public static String LINE_SPLITER = getLineSpliter();
	public static byte[] lineSpliter = LINE_SPLITER.getBytes(UTF8);
	private static Logger log = LoggerFactory.getLogger(DataWriter.class);
	private ConcurrentHashMap<Long, Integer> lineIndex = new ConcurrentHashMap<>();

	public DataWriter(File file, boolean append) throws Exception {
		this.file = file;
		this.fileId = MetaReadWriter.getFileId(file);
		this.pointer = file.length();
		this.lineNo = readLineNo(file);
		this.dirId = Integer.parseInt(file.getParentFile().getName());
		this.writer = new BufferedOutputStream(new FileOutputStream(file, append));
	}

	private static String getLineSpliter() {
		String os = System.getProperty("os.name").toLowerCase();
		return os.contains("windows") ? "\n" : "\r";
	}

	public int writeLine(String str) throws IOException {
		lineIndex.put(lineNo, (int) pointer);
		byte[] bytes = str.getBytes(UTF8);
		writer.write(bytes);
		writer.write(lineSpliter);
		int writeSize = bytes.length + lineSpliter.length;
		pointer += writeSize;
		lineNo++;
		change.set(true);
		return writeSize;
	}

	public File getFile() {
		return file;
	}

	public long getPointer() {
		return pointer;
	}

	public long getlineNo() {
		return lineNo;
	}

	@Override
	public void close() throws Exception {
		lineIndex.clear();
		if (writer != null)
			writer.close();
	}

	private long readLineNo(File file) {
		if (!file.exists())
			return 0;
		int pos = 0;
		long line = 0;
		long st = System.currentTimeMillis();
		try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file))) {
			int size = 0;
			lineIndex.put(line++, 0);
			byte[] buf = new byte[2048];
			while ((size = stream.read(buf)) != -1) {
				for (int i = 0; i < size; i++) {
					if (buf[i] == '\n' || buf[i] == '\r') {
						lineIndex.put(line++, (pos + i + 1));
					}
				}
				pos += size;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		long end = System.currentTimeMillis();
		log.info("count:{} line from:{} time:{}ms", line, file, end - st);
		return line - 1;
	}

	public int getDirId() {
		return dirId;
	}

	public void flush() {
		if (change.get()) {
			try {
				writer.flush();
				change.set(false);
			} catch (IOException e) {
				log.error("flus error", e);
			}
		}
	}

	public int getFileId() {
		return fileId;
	}

	public long getLinePost(long lineNo) {
		if (lineNo >= lineIndex.size())
			return file.length();
		return lineIndex.get(lineNo);
	}

}
