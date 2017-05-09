package com.czp.ulc.test;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.common.meta.MetaReadWriter;
import com.czp.ulc.common.util.Utils;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月3日 上午9:56:01</li>
 * 
 * @version 0.0.1
 */

public class DataCompressWriterTest {

	private MetaReadWriter mw;

	@Before
	public void init() throws Exception {
		// mw = new MetaReadWriter("./test");
	}

	@Test
	public void testEncodeId() {
		byte[] metaId = MetaReadWriter.encodeMetaId(20170508, 11, 1175);
		System.out.println(Arrays.toString(metaId));
		long[] ids = MetaReadWriter.decodeMetaId(metaId);
		long x = ids[0];
		int dirId = (int) (x >> 32);
		int fileId = (int) x;
		System.out.println(dirId+"--->"+fileId);
		System.out.println(Arrays.toString(ids));
	}

	@Test
	public void testCompress() throws IOException {
		File file = new File("./log/data/20170508/0.log");
		if (file.exists()) {
			// File outPut = new File("./test/0.log.zip");
			// for (int i = 0; i < 1; i++) {
			// mw.doCompress(file, outPut, false);
			// }
			// 1816-->774720
			// 1817-->775236
			FileInputStream fis = new FileInputStream(file);
			fis.skip(774720);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			System.out.println(br.readLine());
			fis.close();
		}
	}

	@Test
	public void testLoadLine() throws IOException {
		File file = new File("./tmp");
		if (!file.exists())
			return;
		System.out.println(mw.loadLineCount(file));
	}

	@Test
	public void testReadLinePos() throws IOException {
		File file = new File("./log//data/20170508/0.log.index");
		if (!file.exists())
			return;
		int line = 0;
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		byte[] buf = new byte[8];
		while (bis.read(buf) != -1) {
			System.out.println(line + "-->" + Utils.bytesToLong(buf));
			line++;
		}
		bis.close();
	}

	@Test
	public void testReader() throws Exception {
		// String file = "./tmp/old/0.log";
		// List<JSONObject> lineRequest = new LinkedList<JSONObject>();
		// JSONObject json = new JSONObject();
		// json.put("f", file);
		// json.put("l", 200);
		// json.put("s", 10);
		// lineRequest.add(json);
		//
		// JSONObject json2 = new JSONObject();
		// json2.put("f", file);
		// json2.put("l", 2);
		// json2.put("s", 1);
		// lineRequest.add(json2);
		// Map<String, Map<Long, String>> mergeRead = mw.mergeRead(lineRequest);
		// Set<Entry<String, Map<Long, String>>> entrySet =
		// mergeRead.entrySet();
		// for (Entry<String, Map<Long, String>> entry : entrySet) {
		// Map<Long, String> value = entry.getValue();
		// Set<Entry<Long, String>> entrySet2 = value.entrySet();
		// for (Entry<Long, String> entry2 : entrySet2) {
		// System.out.println("line:" + entry2.getKey());
		// System.out.println("\t" + entry2.getValue());
		// }
		// }
	}
}
