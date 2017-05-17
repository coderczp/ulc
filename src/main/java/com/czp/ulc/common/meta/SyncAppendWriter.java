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
 * 追加写 <br>
 * <li>创建人：Jeff.cao</li> <br>
 * <li>创建时间：2017年5月17日 上午9:08:36<>
 * 
 * @version 0.0.1
 */

public class SyncAppendWriter implements ShutdownCallback {

	private File baseDir;
	private volatile File currentFile;
	private volatile BufferedOutputStream stream;
	private AtomicLong postion = new AtomicLong(0);
	private FileChangeListener fileChangeListener;

	public static final String SUFFX = ".log";
	public static final int EACH_FILE_SIZE = 1024 * 1024 * 250;
	public static final FilenameFilter FILTER = Utils.newFilter(SUFFX);
	private static final Logger LOG = LoggerFactory.getLogger(SyncAppendWriter.class);

	public SyncAppendWriter(File baseDir, FileChangeListener fileChangeListener) {
		this.baseDir = baseDir;
		this.stream = getCurrentStream();
		this.fileChangeListener = fileChangeListener;
		ShutdownManager.getInstance().addCallback(this);
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
					fileChangeListener.onFileChange(currentFile);
					stream.close();
					stream = getCurrentStream();
				}
			}
		}
		stream.write(bytes);
		return postion.getAndAdd(bytes.length);

	}

	public File currentFile() {
		return currentFile;
	}

	public File[] getAllFiles() {
		return baseDir.listFiles(FILTER);
	}

	/***
	 * 获取当前channel
	 * 
	 * @return
	 */
	private BufferedOutputStream getCurrentStream() {
		try {
			currentFile = chooseFile();
			LOG.info("use  file:{}", currentFile);
			postion.set(currentFile.length());
			return new BufferedOutputStream(new FileOutputStream(currentFile, true));
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	/***
	 * 获取当文件
	 * 
	 * @return
	 */
	private File chooseFile() {
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
	private int getFileId(File file) {
		String name = file.getName();
		return Integer.parseInt(name.substring(0, name.indexOf(".")));
	}

	@Override
	public void onSystemExit() {
		Utils.close(stream);
	}

	public void close() {
		Utils.close(stream);
	}

	public boolean isHistoryFile(File file) {
		return file.length() >= EACH_FILE_SIZE;
	}

}
