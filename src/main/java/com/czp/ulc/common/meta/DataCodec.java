package com.czp.ulc.common.meta;

import java.nio.ByteBuffer;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月11日 上午11:18:10</li>
 * 
 * @version 0.0.1
 */

public class DataCodec {

	public static final int DATA_LEN_BYTE_SIZE = 2;

	/***
	 * 编码为1字节(记录长度几个字节)+字节长度+内容+换行
	 * 
	 * @param data
	 * @param endByte
	 * @return
	 */
	public static byte[] encode(byte[] data, byte[] endByte) {
		int len = data.length + endByte.length;
		if(len>Character.MAX_VALUE){
			throw new RuntimeException("eacn data size must be <65535");
		}
		ByteBuffer buf = ByteBuffer.allocate(2 + len);
		buf.putChar((char) len);
		buf.put(data);
		buf.put(endByte);
		buf.flip();
		return buf.array();
	}

	/***
	 * 4字节目录编号 4字节文件编号 x字节行号(<128:1byte 65536:2byte ....)<br>
	 * 将文件id和目录ID编码为一个long
	 * 
	 * @param dirId
	 * @param fileId
	 * @param lineNo
	 * @return
	 */
	public static byte[] encodeMetaId(int dirId, int fileId, int lineNo) {
		ByteBuffer buf = ByteBuffer.allocate(12);
		buf.putInt(dirId);
		buf.putInt(fileId);
		if (lineNo < Byte.MAX_VALUE) {
			buf.put((byte) lineNo);
		} else if (lineNo < Character.MAX_VALUE) {
			buf.putChar((char) lineNo);
		} else {
			buf.putLong(lineNo);
		}
		buf.flip();
		byte[] realArr = new byte[buf.limit()];
		System.arraycopy(buf.array(), 0, realArr, 0, buf.limit());
		return realArr;
	}
}
