package com.czp.ulc.module.lucene;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.util.Utils;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月18日 上午9:25:45</li>
 * 
 * @version 0.0.1
 */

public class RollingWriter implements AutoCloseable {

	protected File baseDir;
	protected String suffx;
	protected long eachFileMaxSize;
	protected FilenameFilter filter;
	protected volatile File currentFile;
	protected final RollingWriteResult result;
	protected volatile BufferedOutputStream stream;
	protected AtomicLong postion = new AtomicLong();

	protected static final Logger LOG = LoggerFactory.getLogger(RollingWriter.class);

	public RollingWriter(File baseDir) {
		this(baseDir, ".log", 1024 * 1024 * 200);
	}

	public RollingWriter(File baseDir, long maxSize) {
		this(baseDir, ".log", maxSize);
	}

	public RollingWriter(File baseDir, String suffx, long maxSize) {
		this.suffx = suffx;
		this.baseDir = baseDir;
		this.eachFileMaxSize = maxSize;
		this.stream = getCurrentStream();
		this.filter = Utils.newFilter(suffx);
		// 初始化一个文件未修改的result,文件变更时重新new一个
		this.result = new RollingWriteResult(false, currentFile, currentFile);
	}

	/***
	 * 获取当文件
	 * 
	 * @return
	 */
	protected File chooseFile() {
		int num = 0;
		File[] files = baseDir.listFiles(filter);
		if (files == null) {
			synchronized (this) {
				File file = new File(baseDir, String.format("%s%s", num, suffx));
				if (!file.exists())
					return file;
			}
		}
		for (File file : files) {
			String name = file.getName();
			if (!isLogDataFile(name))
				continue;

			num = Math.max(num, getFileId(name));
			if (file.length() < eachFileMaxSize)
				break;
			num++;
		}

		return new File(baseDir, String.format("%s%s", num, suffx));
	}

	/***
	 * 检查是否是0.log/1.log/2.log
	 * 
	 * @param file
	 * @return
	 */
	private boolean isLogDataFile(String name) {
		return Character.isDigit(name.charAt(0));
	}

	/**
	 * 根据文件名取ID
	 * 
	 * @param file
	 * @return
	 */
	public int getFileId(String name) {
		return Integer.parseInt(name.substring(0, name.indexOf(".")));
	}

	public File[] getAllFiles() {
		return baseDir.listFiles(filter);
	}

	public File getCurrentFile() {
		return currentFile;
	}

	public RollingWriteResult append(byte[] bytes) throws IOException {
		return append(bytes, 0, bytes.length,false);
	}

	/***
	 * 写数据并返回当前指针
	 * 
	 * @param bytes
	 * @return
	 * @throws IOException
	 */
	public RollingWriteResult append(byte[] bytes, int offset, int len, boolean addLine) throws IOException {
		long startPos = postion.get();
		result.setPostion(startPos);
		if (startPos >= eachFileMaxSize) {
			synchronized (this) {
				if (postion.get() >= eachFileMaxSize) {
					stream.write(bytes, offset, len);
					stream.close();
					File tmp = currentFile;
					stream = getCurrentStream();
					result.setCurrentFile(currentFile);
					return new RollingWriteResult(true, tmp, tmp);
				}
			}
		}
		stream.write(bytes, offset, len);
		if (addLine) {
			len++;
			stream.write('\n');
		}
		postion.getAndAdd(len);
		return result;

	}

	/***
	 * 获取当前channel
	 * 
	 * @return
	 */
	private BufferedOutputStream getCurrentStream() {
		try {
			currentFile = chooseFile();
			postion.set(currentFile.length());
			LOG.info("use file:{}", currentFile);
			return new BufferedOutputStream(new FileOutputStream(currentFile, true));
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	@Override
	public void close() {
		Utils.close(stream);
	}
}
