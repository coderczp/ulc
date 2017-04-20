package com.czp.ulc.common.lucene;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.lucene.store.IndexInput;

/**
 * @dec Function
 * @author coder_czp@126.com
 * @date 2017年4月7日/下午7:33:54
 * @copyright coder_czp@126.com
 *
 */
public class ZipIndexInput extends IndexInput {

	private InputStream stream;
	private ZipEntry entry;
	private ZipFile file;
	private long position;
	private long size;
	protected String name;

	protected ZipIndexInput(String resourceDescription, ZipFile f, long offset, long size) throws IOException {
		super(resourceDescription);
		this.name = resourceDescription;
		this.entry = f.getEntry(resourceDescription);
		init(entry, f);
		if (offset >= 0)
			this.position = offset;
		if (size > 0)
			this.size = size;
		else
			this.size = entry.getSize();

	}

	public synchronized byte readByte() throws IOException {
		if (position >= size)
			throw new IOException();

		int res = -1;
		if ((res = stream.read()) == -1) {
			throw new IOException();
		}
		position++;
		return (byte) res;
	}

	public synchronized void readBytes(byte[] b, int offset, int len) throws IOException {
		if (position >= size || position + (len - size) >= size)
			throw new IOException();
		position += stream.read(b, offset, len);
	}

	public void close() throws IOException {
		position = -1;
		stream.close();
	}

	public long getFilePointer() {
		return position;
	}

	public void seek(long pos) throws IOException {
		if (pos < position) {
			resetStream();
			stream.skip(pos);
			position = pos;
		} else {
			long togo = pos - position;
			stream.skip(togo);
			position = pos;
		}
	}

	public long length() {
		return entry.getSize();
	}

	private void resetStream() throws IOException {
		System.out.println(entry.getName());
		stream = file.getInputStream(entry);
		position = 0;
	}

	private void init(ZipEntry e, ZipFile f) throws IOException {
		entry = e;
		file = f;
		resetStream();
	}

	@Override
	public IndexInput slice(String sliceDescription, long offset, long length) throws IOException {
		return new ZipIndexInput(sliceDescription, file, offset, length);
	}
}
