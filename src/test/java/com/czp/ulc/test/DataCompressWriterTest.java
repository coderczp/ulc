package com.czp.ulc.test;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.czp.ulc.common.meta.MetaCompressManager;

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
		MetaCompressManager instance = MetaCompressManager.getInstance();
		instance.doCompress(src, outPut, true);
		instance.onSystemExit();
	}

	//@Test
	public void testReader() throws Exception {
		int size = 4;
		String zipItem = "0.log";
		File zipFile = new File("./log/data/20170504.zip");
		long lineStart = 10;
		MetaCompressManager instance = MetaCompressManager.getInstance();
		List<String> lines = instance.readFromCompressFile(zipFile, lineStart, size);
		for (String string : lines) {
			System.out.println(string);
		}
		instance.onSystemExit();
	}
}
