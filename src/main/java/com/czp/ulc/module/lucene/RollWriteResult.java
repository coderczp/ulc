package com.czp.ulc.module.lucene;

import java.io.File;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月11日 下午6:13:36</li>
 * 
 * @version 0.0.1
 */

public class RollWriteResult {

	private volatile long postion;
	private volatile File lastFile;
	private volatile File currentFile;
	private volatile boolean fileChanged;

	public RollWriteResult(boolean fileChanged, File lastFile,File currentFile) {
		this.lastFile = lastFile;
		this.fileChanged = fileChanged;
		this.currentFile = currentFile;
	}

	public long getPostion() {
		return postion;
	}

	public void setPostion(long postion) {
		this.postion = postion;
	}

	public File getCurrentFile() {
		return currentFile;
	}

	public void setCurrentFile(File currentFile) {
		this.currentFile = currentFile;
	}

	public void setFileChanged(boolean fileChanged) {
		this.fileChanged = fileChanged;
	}

	public boolean isFileChanged() {
		return fileChanged;
	}

	public void setLastFile(File lastFile) {
		this.lastFile = lastFile;
	}

	public File getLastFile() {
		return lastFile;
	}

	@Override
	public String toString() {
		return "RollingWriteResult [postion=" + postion + ", currentFile=" + currentFile + "]";
	}

}
