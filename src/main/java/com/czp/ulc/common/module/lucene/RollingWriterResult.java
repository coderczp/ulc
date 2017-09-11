package com.czp.ulc.common.module.lucene;

import java.io.File;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月11日 下午6:13:36</li>
 * 
 * @version 0.0.1
 */

public class RollingWriterResult {

	private File lastFile;

	private volatile boolean fileChanged;

	public RollingWriterResult(boolean fileChanged, File lastFile) {
		this.fileChanged = fileChanged;
		this.lastFile = lastFile;
	}

	public void setFileChanged(boolean fileChanged) {
		this.fileChanged = fileChanged;
	}

	public boolean isFileChanged() {
		return fileChanged;
	}

	public File getLastFile() {
		return lastFile;
	}

}
