package com.czp.ulc.common.meta;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.czp.ulc.common.util.Utils;

/**
 * 追加写 <br>
 * <li>创建人：Jeff.cao</li> <br>
 * <li>创建时间：2017年5月17日 上午9:08:36<>
 * 
 * @version 0.0.1
 */

public class SyncWriter extends AbstractWriter {

	private volatile BufferedOutputStream stream;

	public SyncWriter(File baseDir, FileChangeListener fileChangeListener) {
		super(baseDir, fileChangeListener);
		this.stream = getCurrentStream();
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

	public void close() {
		Utils.close(stream);
	}

}
