package com.czp.ulc.common.meta;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.common.shutdown.ShutdownManager;
import com.czp.ulc.common.util.Utils;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月18日 上午9:25:45</li>
 * 
 * @version 0.0.1
 */

public abstract class AbstractWriter implements RollingWriter {

	protected File baseDir;
	protected volatile File currentFile;
	protected FileChangeListener fileChangeListener;
	protected AtomicLong postion = new AtomicLong();

	public static final String SUFFX = ".log";
	public static final int EACH_FILE_SIZE = 1024 * 1024 * 200;
	public static final FilenameFilter FILTER = Utils.newFilter(SUFFX);
	protected static final Logger LOG = LoggerFactory.getLogger(SyncWriter.class);

	public AbstractWriter(File baseDir, FileChangeListener fileChangeListener) {
		this.baseDir = baseDir;
		this.fileChangeListener = fileChangeListener;
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

	@Override
	public void onSystemExit() {
		try {
			close();
		} catch (Exception e) {
			LOG.error("close error", e);
		}
	}

	public File[] getAllFiles() {
		return baseDir.listFiles(FILTER);
	}

	public boolean isHistoryFile(File file) {
		return file.length() >= EACH_FILE_SIZE;
	}

	@Override
	public File getCurrentFile() {
		return currentFile;
	}

}
