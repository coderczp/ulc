package com.czp.ulc.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

import com.czp.ulc.common.meta.AnsyWriter;
import com.czp.ulc.common.meta.FileChangeListener;
import com.czp.ulc.common.meta.RollingWriter;
import com.czp.ulc.common.meta.SyncWriter;

/**
 * 请添加描述 <li>创建人：Jeff.cao</li> <li>创建时间：2017年5月17日 上午10:43:26</li>
 * 
 * @version 0.0.1
 */

public class AnsyAppendWriterTest {

	@Test
	public void testAysnWrite() throws IOException {
		long st = System.currentTimeMillis();
		AnsyWriter writer = new AnsyWriter(new File("log"), null);
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
	public void testSyncWrite() throws Exception {
		long st = System.currentTimeMillis();
		RollingWriter writer = new SyncWriter(new File("log"), new FileChangeListener() {

			@Override
			public void onFileChange(File currentFile) {
				System.out.println(currentFile);
			}
		});
		BufferedReader lines = Files.newBufferedReader(new File("./3.log").toPath());
		String line = null;
		while ((line = lines.readLine()) != null) {
			writer.append(line.getBytes());
		}
		lines.close();
		writer.close();
		System.out.println("nio:" + (System.currentTimeMillis() - st));
	}
}
