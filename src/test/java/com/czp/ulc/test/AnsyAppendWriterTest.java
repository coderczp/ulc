package com.czp.ulc.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

import com.czp.ulc.common.meta.AnsyAppendWriter;
import com.czp.ulc.common.meta.AppendWriter;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月17日 上午10:43:26</li>
 * 
 * @version 0.0.1
 */

public class AnsyAppendWriterTest {

	@Test
	public void testAysnWrite() throws IOException {
		long st = System.currentTimeMillis();
		AnsyAppendWriter writer = new AnsyAppendWriter(new File("log"));
		BufferedReader lines = Files.newBufferedReader(new File("./3.log").toPath());
		String line = null;
		while ((line = lines.readLine()) != null) {
			byte[] bytes = line.getBytes();
			writer.append(bytes);
		}
		lines.close();
		writer.close();
		System.out.println("aio:" + (System.currentTimeMillis() - st));
	}

	@Test
	public void testSyncWrite() throws IOException {
		long st = System.currentTimeMillis();
		AppendWriter writer = new AppendWriter(new File("log"));
		BufferedReader lines = Files.newBufferedReader(new File("./3.log").toPath());
		String line = null;
		while ((line = lines.readLine()) != null) {
			writer.append(line);
		}
		lines.close();
		writer.close();
		System.out.println("nio:" + (System.currentTimeMillis() - st));
	}
}
