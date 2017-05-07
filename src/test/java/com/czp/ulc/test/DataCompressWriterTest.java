package com.czp.ulc.test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.common.meta.MetaReadWriter;
import com.czp.ulc.common.util.Utils;

/**
 * 请添加描述 <li>创建人：Jeff.cao</li> <li>创建时间：2017年5月3日 上午9:56:01</li>
 * 
 * @version 0.0.1
 */

public class DataCompressWriterTest {

	 @Test
	public void testCompress() throws IOException {
		File outPut = new File("/Users/itrip/Documents/xlog/0.log.index");
		System.out.println(outPut.length()/1024.0/1024);
		BufferedInputStream fis = new BufferedInputStream(new FileInputStream(outPut));
		byte[] buf = new byte[Long.BYTES];
		int i = 0;
		while(fis.read(buf)!=-1&&i<200){
			System.out.println(i+"---"+Utils.bytesToLong(buf));
			i++;
		}
		fis.close();
	}

	@Test
	public void testReader() throws Exception {
		// File zipFile = new File("/Users/itrip/Documents/0.log.zip");
		List<JSONObject> lineRequest = new LinkedList<JSONObject>();
		JSONObject json = new JSONObject();
		json.put("f", "/Users/itrip/Documents/xlog/0.log");
		json.put("l", 200);
		json.put("s", 10);
		lineRequest.add(json);
		
		JSONObject json2 = new JSONObject();
		json2.put("f", "/Users/itrip/Documents/xlog/0.log");
		json2.put("l", 2);
		json2.put("s", 5);
		lineRequest.add(json2);
		Map<String, Map<Long, String>> mergeRead = MetaReadWriter.mergeRead(lineRequest);
		Set<Entry<String, Map<Long, String>>> entrySet = mergeRead.entrySet();
		for (Entry<String, Map<Long, String>> entry : entrySet) {
			Map<Long, String> value = entry.getValue();
			Set<Entry<Long, String>> entrySet2 = value.entrySet();
			for (Entry<Long, String> entry2 : entrySet2) {
				System.out.println("line:"+entry2.getKey());
				System.out.println("\t"+entry2.getValue());
			}
		}
	}
}
