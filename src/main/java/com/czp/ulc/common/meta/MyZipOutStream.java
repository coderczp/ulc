package com.czp.ulc.common.meta;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @dec Function
 * @author coder_czp@126.com
 * @date 2017年5月21日/下午10:30:07
 * @copyright coder_czp@126.com
 *
 */
public class MyZipOutStream extends GZIPOutputStream {

	public MyZipOutStream(OutputStream out) throws IOException {
		super(out);
	}

	public long writeData(byte[] data) throws IOException {
		write(data);
		return def.getBytesWritten();
	}

}
