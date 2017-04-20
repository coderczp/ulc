package com.czp.ulc.common.lucene;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

import org.apache.lucene.store.IndexOutput;

/**
 * @dec Function
 * @author coder_czp@126.com
 * @date 2017年4月7日/下午7:59:05
 * @copyright coder_czp@126.com
 *
 */
public class ZipIndexOutput extends IndexOutput {

	private long bytesWritten = 0L;
	private final CRC32 crc = new CRC32();
	private final BufferedOutputStream os;

	protected ZipIndexOutput(String resourceDescription, String name, OutputStream out) {
		super(resourceDescription, name);
		this.os = new BufferedOutputStream(new CheckedOutputStream(out, crc), 1024);
	}

	@Override
	public void close() throws IOException {
		os.flush();
	}

	@Override
	public long getFilePointer() {
		return bytesWritten;
	}

	@Override
	public long getChecksum() throws IOException {
		return crc.getValue();
	}

	@Override
	public void writeByte(byte b) throws IOException {
		os.write(b);
		bytesWritten++;
	}

	@Override
	public void writeBytes(byte[] b, int offset, int length) throws IOException {
		os.write(b, offset, length);
		bytesWritten += length - offset;
	}

}
