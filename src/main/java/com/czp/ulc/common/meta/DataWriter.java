package com.czp.ulc.common.meta;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.common.util.Utils;

/**
 * 请添加描述 <li>创建人：Jeff.cao</li> <li>创建时间：2017年5月4日 上午11:27:03</li>
 * 
 * @version 0.0.1
 */

public class DataWriter implements AutoCloseable {

	private File file;
	private AtomicLong lineNo;
	private AtomicLong pointer;
	private BufferedOutputStream writer;
	private BufferedOutputStream indexWriter;

	public static Charset UTF8 = Charset.forName("UTF-8");
	public static String LINE_SPLITER = System.lineSeparator();
	public static byte[] lineSpliter = LINE_SPLITER.getBytes(UTF8);
	private static Logger log = LoggerFactory.getLogger(DataWriter.class);

	public DataWriter(File file, boolean append) throws Exception {
		String indexFile = MetaReadWriter.getIndexFileName(file);
		FileOutputStream out = new FileOutputStream(new File(file.getParent(), indexFile));
		this.indexWriter = new BufferedOutputStream(out);
		this.file = file;
		this.lineNo = readLineNo(file);
		this.pointer = new AtomicLong(file.length());
		this.writer = new BufferedOutputStream(new FileOutputStream(file, append));
	}

	public int writeLine(String str) throws IOException {
		byte[] bytes = str.getBytes(UTF8);
		writer.write(bytes);
		writer.write(lineSpliter);
		int writeSize = bytes.length + lineSpliter.length;
		pointer.addAndGet(writeSize);
		lineNo.getAndIncrement();
		return writeSize;
	}

	public File getFile() {
		return file;
	}

	public long getPointer() {
		return pointer.get();
	}

	public long getlineNo() {
		return lineNo.get();
	}

	@Override
	public void close() throws Exception {
		if (writer != null)
			writer.close();
		if (indexWriter != null)
			indexWriter.close();
	}

	private AtomicLong readLineNo(File file) {
		if (!file.exists())
			return new AtomicLong(0);
		long line = 0, pos = 0;
		long st = System.currentTimeMillis();
		try (BufferedReader stream = Files.newBufferedReader(file.toPath())) {
			String lineStr = null;
			while ((lineStr = stream.readLine()) != null) {
				indexWriter.write(Utils.longToBytes(pos));
				pos += lineStr.length();
				line++;
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getCause());
		}
		long end = System.currentTimeMillis();
		log.info("count:{} line from:{} time:{}ms", line, file, end - st);
		return new AtomicLong(line);
	}

	public void writeIndex(long lineNo, long pointer) throws IOException {
		indexWriter.write(Utils.longToBytes(pointer));
	}
}
