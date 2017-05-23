package com.czp.ulc.test;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.Test;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

/**
 * @dec Function
 * @author coder_czp@126.com
 * @date 2017年5月13日/下午6:23:12
 * @copyright coder_czp@126.com
 *
 */
public class TestCompress {

	@Test
	public void testGzipWrite() throws IOException {
		long st = System.currentTimeMillis();
		GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream("./test.gz"));
		BufferedReader lines = Files.newBufferedReader(new File("./9.log").toPath());
		String line = null;
		while ((line = lines.readLine()) != null) {
			byte[] bytes = line.getBytes();
			gzos.write(bytes);
		}
		lines.close();
		gzos.close();
		System.out.println(System.currentTimeMillis() - st);
	}

	@Test
	public void testZstdWrite() throws IOException {
		long st = System.currentTimeMillis();
		ZstdOutputStream gzos = new ZstdOutputStream(new FileOutputStream("./test.zs"));
		BufferedReader lines = Files.newBufferedReader(new File("./9.log").toPath());
		String line = null;
		while ((line = lines.readLine()) != null) {
			byte[] bytes = line.getBytes();
			gzos.write(bytes);
		}
		lines.close();
		gzos.close();
		System.out.println(System.currentTimeMillis() - st);
	}

	@Test
	public void testGzipRead() throws FileNotFoundException, IOException {
		GZIPInputStream gz = new GZIPInputStream(new FileInputStream("./test.gz"));
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("./gz.log"));
		byte[] buf = new byte[1024];
		int n = -1;
		while ((n = gz.read(buf)) != -1) {
			bos.write(buf, 0, n);
		}
		bos.close();
		gz.close();
	}

	@Test
	public void testZstdRead() throws FileNotFoundException, IOException {
		ZstdInputStream gz = new ZstdInputStream(new FileInputStream("./test.zs"));
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("./zs.log"));
		byte[] buf = new byte[1024];
		int n = -1;
		while ((n = gz.read(buf)) != -1) {
			bos.write(buf, 0, n);
		}
		bos.close();
		gz.close();
	}

}
