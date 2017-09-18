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
	protected volatile File currentFile;
	protected final RollingWriterResult unChange;
	protected volatile BufferedOutputStream stream;
	protected AtomicLong postion = new AtomicLong();

	public static final String SUFFX = ".log";
	public static final int EACH_FILE_SIZE = 1024 * 1024 * 200;
	public static final FilenameFilter FILTER = Utils.newFilter(SUFFX);
	protected static final Logger LOG = LoggerFactory.getLogger(RollingWriter.class);

	public RollingWriter(File baseDir) {
		this.baseDir = baseDir;
		this.stream = getCurrentStream();
		// 初始化一个文件未修改的result,文件变更时重新new一个
		this.unChange = new RollingWriterResult(false, currentFile);
	}

	/***
	 * 获取当文件
	 * 
	 * @return
	 */
	protected File chooseFile() {
		int num = 0;
		for (File file : baseDir.listFiles(FILTER)) {
			String name = file.getName();
			if (!isLogDataFile(name))
				continue;

			num = Math.max(num, getFileId(name));
			// 不检测大小,因为进程异常退出后文件可能没有达到EACH_FILE_SIZE
			// if (file.length() >= EACH_FILE_SIZE)
			num++;
		}
		return new File(baseDir, String.format("%s%s", num, SUFFX));
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
		return baseDir.listFiles(FILTER);
	}

	public File getCurrentFile() {
		return currentFile;
	}

	/***
	 * 写数据并返回当前指针
	 * 
	 * @param bytes
	 * @return
	 * @throws IOException
	 */
	public RollingWriterResult append(byte[] bytes) throws IOException {
		if (postion.get() >= EACH_FILE_SIZE) {
			synchronized (this) {
				if (postion.get() >= EACH_FILE_SIZE) {
					stream.close();
					File tmp = currentFile;
					stream = getCurrentStream();
					return new RollingWriterResult(true, tmp);
				}
			}
		}
		stream.write(bytes);
		postion.getAndAdd(bytes.length);
		return unChange;

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
