package com.czp.ulc.test;

import java.io.File;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月3日 上午9:56:01</li>
 * 
 * @version 0.0.1
 */

public class DataCompressWriterTest {

	//@Test
	public void testCompress() {
		File src = new File("./log/data/20170504");
		File outPut = new File("./log/data/20170504.zip");
	}

	//@Test
	public void testReader() throws Exception {
		int size = 4;
		String zipItem = "0.log";
		File zipFile = new File("./log/data/20170504.zip");
		long lineStart = 10;
	}
}
