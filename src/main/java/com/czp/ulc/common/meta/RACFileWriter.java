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
import java.nio.ByteBuffer;
import java.util.zip.DeflaterOutputStream;

/**
 * Function:RandomAccessCompres支持随机访问的压缩文件
 *
 * @date:2017年5月22日/下午5:00:34
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class RACFileWriter {

	/** 压缩数据偏移 */
	private int offset;
	/** 未压缩的数据大小 */
	private int srcSize;
	private OutputStream out;
	public static final int len = 1024 * 1024;
	public static final int maxLen = len + len / 4 + 4;
	private MemOutStream buf = new MemOutStream(maxLen);
	private DeflaterOutputStream dos = new DeflaterOutputStream(buf);

	public RACFileWriter(OutputStream out) throws IOException {
		this.out = out;
		this.writeHeaderLenPlacholder();

	}

	public int writeAndReturnBlock(byte[] buf) throws IOException {
		checkCompressBlockIsFull();
		dos.write(buf);
		srcSize += buf.length;
		return offset;
	}

	private void checkCompressBlockIsFull() throws IOException {
		int size = buf.size();
		if (size >= len) {
			flush();
			writeHeaderLenPlacholder();
			dos = new DeflaterOutputStream(buf);
		}
	}

	private void flush() throws IOException {
		dos.close();
		offset += buf.size();
		writeHeaderLen(buf.size(), srcSize);
		buf.writeTo(out);
		buf.reset();
		srcSize = 0;
	}

	public int closeAndReturnBlock() {
		int size = buf.size();
		if (size >= 0) {
			try {
				flush();
				out.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}
		return offset;
	}

	// buf的前四个字节为当前压缩快的大小,每次切换时预留
	private void writeHeaderLenPlacholder() throws IOException {
		// 这里只能用wirte方法,需要移动写指针
		buf.write(intToBytes(0));
		buf.write(intToBytes(0));
	}

	// 写入压缩块大小和原始数据大小
	private void writeHeaderLen(int blockLen, int unCompressLen) throws IOException {
		buf.orrvide(0, intToBytes(blockLen));
		buf.orrvide(4, intToBytes(unCompressLen));
	}

	private byte[] intToBytes(int num) {
		return ByteBuffer.allocate(4).putInt(num).array();
	}

}
