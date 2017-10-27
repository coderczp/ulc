package com.czp.ulc.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.nio.file.Files;

import org.junit.Test;

import com.czp.ulc.module.conn.MyBufferReader;
import com.czp.ulc.module.lucene.RollingWriteResult;
import com.czp.ulc.module.lucene.RollingWriter;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月17日 上午10:43:26</li>
 * 
 * @version 0.0.1
 */

public class AnsyAppendWriterTest {

	@Test
	public void testSyncWrite() throws Exception {
		long st = System.currentTimeMillis();
		File baseDir = new File("data");
		baseDir.mkdirs();
		
		RollingWriter writer = new RollingWriter(baseDir,1024*1024*10L);
		BufferedReader lines = Files.newBufferedReader(new File("index/data/31.log").toPath());
		String line = null,lastLine = null;
		RollingWriteResult append = null;
		while ((line = lines.readLine()) != null) {
			byte[] bytes = line.getBytes();
			append = writer.append(bytes);
			System.out.println(append);
			lastLine = line;
		}
		lines.close();
		writer.close();
		System.out.println("nio:" + (System.currentTimeMillis() - st));
		System.out.println(lastLine);
		RandomAccessFile f = new RandomAccessFile(append.getCurrentFile(), "r");
		f.seek(append.getPostion());
		byte[] b = new byte[lastLine.length()];
		f.read(b, 0, b.length);
		System.out.println(new String(b));
		f.close();
	}
	
	public static void main(String[] args) throws Exception {
		MyBufferReader mb = new MyBufferReader(new FileReader("src/main/resources/db.properties"));
		String line = null;
		while ((line = mb.readLine()) != null) {
			System.out.print(line);
		}
		mb.close();
	}
}
