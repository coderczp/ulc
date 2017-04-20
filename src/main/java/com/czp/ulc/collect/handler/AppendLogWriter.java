package com.czp.ulc.collect.handler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * WAL
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月11日 下午3:01:47</li>
 * 
 * @version 0.0.1
 */

public class AppendLogWriter implements AutoCloseable {

	/** 写入的文件 */
	private File file;

	/** 已经写入的字节 */
	private long writeBytes;

	private BufferedWriter writer;

	public AppendLogWriter(File out) throws IOException {
		this.file = out;
		this.writeBytes = out.length();
		this.writer = new BufferedWriter(new FileWriter(out));
	}

	public long getWriteBytes() {
		return writeBytes;
	}

	public File getFile() {
		return file;
	}

	public void appendWithLine(int id, String line) throws IOException {
		StringBuffer tmp = new StringBuffer(String.valueOf(id)).append("_").append(line).append("\n");
		writer.write(tmp.toString());
		writeBytes += line.length() + 1;
	}

	public void flush() throws IOException {
		writer.flush();
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

}
