package com.czp.ulc.common.meta;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.common.shutdown.ShutdownCallback;
import com.czp.ulc.common.shutdown.ShutdownManager;
import com.czp.ulc.common.util.Utils;

/**
 * 请添加描述 <li>创建人：Jeff.cao</li> <li>创建时间：2017年5月18日 上午9:25:45</li>
 * 
 * @version 0.0.1
 */

public class RollingWriter implements AutoCloseable, ShutdownCallback {

	protected File baseDir;
	protected volatile File currentFile;
	protected FileChangeListener listener;
	protected volatile BufferedOutputStream stream;
	protected AtomicLong postion = new AtomicLong();

	public static final String SUFFX = ".log";
	public static final int EACH_FILE_SIZE = 1024 * 1024 * 200;
	public static final FilenameFilter FILTER = Utils.newFilter(SUFFX);
	protected static final Logger LOG = LoggerFactory.getLogger(RollingWriter.class);

	public RollingWriter(File baseDir, FileChangeListener listener) {
		this.baseDir = baseDir;
		this.listener = listener;
		this.stream = getCurrentStream();
		ShutdownManager.getInstance().addCallback(this);
	}

	/***
	 * 获取当文件
	 * 
	 * @return
	 */
	protected File chooseFile() {
		int num = 0;
		for (File file : baseDir.listFiles(FILTER)) {
			num = Math.max(num, getFileId(file));
			if (file.length() >= EACH_FILE_SIZE)
				num++;
		}
		return new File(baseDir, String.format("%s%s", num, SUFFX));
	}

	/**
	 * 根据文件名取ID
	 * 
	 * @param file
	 * @return
	 */
	public int getFileId(File file) {
		String name = file.getName();
		return Integer.parseInt(name.substring(0, name.indexOf(".")));
	}

	public File[] getAllFiles() {
		return baseDir.listFiles(FILTER);
	}

	public boolean isHistoryFile(File file) {
		return file.length() >= EACH_FILE_SIZE;
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
	public long append(byte[] bytes) throws IOException {
		if (postion.get() >= EACH_FILE_SIZE) {
			synchronized (this) {
				if (postion.get() >= EACH_FILE_SIZE) {
					stream.close();
					listener.onFileChange(currentFile);
					stream = getCurrentStream();
				}
			}
		}
		stream.write(bytes);
		return postion.getAndAdd(bytes.length);

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

	@Override
	public void onSystemExit() {
		close();
	}
}
