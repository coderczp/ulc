package com.czp.ulc.common.meta;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.common.shutdown.ShutdownCallback;
import com.czp.ulc.common.shutdown.ShutdownManager;
import com.czp.ulc.common.util.Utils;

/**
 * 追加写
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月17日 上午9:08:36</li>
 * 
 * @version 0.0.1
 */

public class AppendWriter implements ShutdownCallback {

	private File baseDir;
	private volatile File currentFile;
	private volatile BufferedWriter writer;
	private AtomicLong postion = new AtomicLong();

	public static final String SUFFX = ".log";
	public static final int EACH_FILE_SIZE = 1024 * 1024 * 250;
	private static final Logger LOG = LoggerFactory.getLogger(AppendWriter.class);

	public AppendWriter(File baseDir) {
		this.baseDir = baseDir;
		this.writer = getCurrentChannle();
		ShutdownManager.getInstance().addCallback(this);
	}

	public long append(String line) throws IOException {
		if (postion.get() >= EACH_FILE_SIZE) {
			synchronized (this) {
				if (postion.get() >= EACH_FILE_SIZE) {
					writer.close();
					writer = getCurrentChannle();
				}
			}
		}
		writer.write(line);
		return postion.getAndAdd(line.length());
	}

	public File getFile() {
		return currentFile;
	}

	/***
	 * 获取当前channel
	 * 
	 * @return
	 */
	private BufferedWriter getCurrentChannle() {
		try {
			currentFile = getCurrentFile();
			LOG.info("use  file:{}", currentFile);
			postion.set(currentFile.length());
			return new BufferedWriter(new FileWriter(currentFile, true));
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	/***
	 * 获取当文件
	 * 
	 * @return
	 */
	private File getCurrentFile() {
		int num = 0;
		for (File file : baseDir.listFiles(Utils.newFilter(SUFFX))) {
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
		Utils.close(writer);
	}

	public void close() {
		Utils.close(writer);
	}
}
