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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.TreeMap;
import java.util.zip.InflaterInputStream;

/**
 * Function:RandomAccessCompres读取rac文件
 *
 * @date:2017年5月22日/下午5:27:30
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class RACFileReader implements AutoCloseable {

	private FileInputStream fis;
	private byte[] infBuffer = new byte[1024];

	public RACFileReader(File racFile) throws IOException {
		this.fis = new FileInputStream(racFile);
	}

	public TreeMap<Integer, byte[]> readLines(int blockOffset, TreeMap<Integer, Integer> lineOffset) throws Exception {
		MemOutStream out = readBlock(blockOffset);
		TreeMap<Integer, byte[]> datas = new TreeMap<>();
		ByteArrayInputStream bis = new ByteArrayInputStream(out.getBuf(), 0, out.size());
		for (Integer integer : lineOffset.keySet()) {
			bis.skip(integer);
			byte[] b = new byte[lineOffset.get(integer)];
			bis.read(b);
			datas.put(integer, b);
			bis.reset();
		}
		return datas;
	}

	public MemOutStream readBlock(int blockOffset) throws IOException {
		if (blockOffset > 0)
			fis.skip(blockOffset);

		fis.read(infBuffer, 0, 8);
		ByteBuffer wrap = ByteBuffer.wrap(infBuffer);
		int count = 0, readSize = 0, unCompressLen = 0;
		wrap.getInt();// 压缩块的大小
		unCompressLen = wrap.getInt();

		MemOutStream out = new MemOutStream(unCompressLen);
		InflaterInputStream is = new InflaterInputStream(fis);
		while ((count = is.read(infBuffer)) != -1 && readSize < unCompressLen) {
			out.write(infBuffer, 0, count);
			readSize += count;
		}
		is.close();
		out.close();
		fis.close();
		return out;
	}

	public void close() throws IOException {
		if (fis != null)
			fis.close();
	}
}
