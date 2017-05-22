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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.Inflater;

import com.czp.ulc.common.util.Utils;

/**
 * Function:RandomAccessCompres读取rac文件
 *
 * @date:2017年5月22日/下午5:27:30
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class RACFileReader {

	private FileInputStream fis;

	public RACFileReader(File racFile) throws IOException {
		this.fis = new FileInputStream(racFile);
	}

	public int readBlock(int blockOffet, byte[] buf) throws Exception {
		fis.skip(blockOffet);
		byte[] blockLen = new byte[4];
		fis.read(blockLen);
		int len = Utils.bytesToInt(blockLen);
		byte[] data = new byte[len];
		fis.read(data);
		Inflater in = new Inflater();
		in.setInput(data);
		in.finished();
		int inflate = in.inflate(buf);
		in.end();
		return inflate;
	}

	public void close() {
		Utils.close(fis);
	}
}
