package com.czp.ulc.common.meta;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.czp.ulc.common.util.Utils;

/**
 * 追加写 <br>
 * <li>创建人：Jeff.cao</li> <br>
 * <li>创建时间：2017年5月17日 上午9:08:36<>
 * 
 * @version 0.0.1
 */

public class AnsyWriter extends AbstractWriter {

	private volatile AsynchronousFileChannel channel;

	public AnsyWriter(File baseDir, FileChangeListener fileChangeListener) {
		super(baseDir, fileChangeListener);
		this.channel = getCurrentChannle();
	}

	/**
	 * 获取当前channel
	 * 
	 * @return
	 */
	private AsynchronousFileChannel getCurrentChannle() {
		try {
			currentFile = chooseFile();
			LOG.info("use  file:{}", currentFile);
			Path path = currentFile.toPath();
			postion.set(currentFile.length());
			return AsynchronousFileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	public void close() {
		Utils.close(channel);
	}

	@Override
	public long append(byte[] bytes) throws IOException {
		if (channel.size() >= EACH_FILE_SIZE) {
			synchronized (this) {
				if (channel.size() >= EACH_FILE_SIZE) {
					channel.close();
					channel = getCurrentChannle();
				}
			}
		}
		long pos = postion.getAndAdd(bytes.length);
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		channel.write(buf, pos);
		return pos;
	}
}
