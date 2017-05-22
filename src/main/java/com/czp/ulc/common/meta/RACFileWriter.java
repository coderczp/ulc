/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.common.meta;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;

import com.czp.ulc.common.util.Utils;

/**
 * Function:RandomAccessCompres支持随机访问的压缩文件
 *
 * @date:2017年5月22日/下午5:00:34
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class RACFileWriter {

	private int offset;
	private OutputStream out;
	public static final int len = 1024 * 1024;
	public static final int maxLen = len + len / 4 + 4;
	private MemOutStream buf = new MemOutStream(maxLen);
	private DeflaterOutputStream dos = new DeflaterOutputStream(buf);

	public RACFileWriter(OutputStream out) throws IOException {
		this.out = out;
		writeCompressBlockLenPlacholder();

	}

	public int writeAndReturnBlock(int b) throws IOException {
		checkCompressBlockIsFull();
		dos.write(b);
		return offset;
	}

	public int writeAndReturnBlock(byte[] data) throws IOException {
		checkCompressBlockIsFull();
		dos.write(data);
		return offset;
	}

	private void checkCompressBlockIsFull() throws IOException {
		int size = buf.size();
		if (size >= len) {
			dos.close();
			offset += buf.size();
			// 写入块大小
			buf.orrvide(0, Utils.intToBytes(size));
			buf.writeTo(out);
			buf.reset();
			writeCompressBlockLenPlacholder();
			dos = new DeflaterOutputStream(buf);

		}
	}

	public int closeAndReturnBlock() throws IOException {
		int size = buf.size();
		if (size >= 0) {
			dos.close();
			buf.writeTo(out);
			out.close();
		}
		return offset;
	}

	// buf的前四个字节为当前压缩快的大小,每次切换时预留
	private void writeCompressBlockLenPlacholder() throws IOException {
		buf.write(Utils.intToBytes(0));

	}
}
