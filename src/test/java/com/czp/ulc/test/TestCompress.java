package com.czp.ulc.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.Test;

import com.czp.ulc.common.meta.RACFileReader;
import com.czp.ulc.common.meta.RACFileWriter;

/**
 * @dec Function
 * @author coder_czp@126.com
 * @date 2017年5月13日/下午6:23:12
 * @copyright coder_czp@126.com
 *
 */
public class TestCompress {

	@Test
	public void testRead2() throws Exception {
		byte[] buf = new byte[RACFileWriter.maxLen];
		RACFileReader r = new RACFileReader(new File("./test.rac"));
		int len = r.readBlock(1051092, buf);
		r.close();
		ByteArrayInputStream bis = new ByteArrayInputStream(buf,0,len);
		long skip = bis.skip(16067945);
		byte[] x = new byte[45];
		bis.read(x,0,x.length);
		System.out.println(new String(x));
	}

	@Test
	public void testWrite2() throws IOException {
		FileOutputStream out = new FileOutputStream("./test.rac");
		BufferedReader lines = Files.newBufferedReader(new File("./3.log").toPath());
		String line = null;
		RACFileWriter rout = new RACFileWriter(out);
		int pos = 0, len = 0, bPos = 0;
		while ((line = lines.readLine()) != null) {
			byte[] bytes = line.getBytes();
			int blockPos = rout.writeAndReturnBlock(bytes);
			if (bPos != blockPos) {
				pos = 0;
				bPos = blockPos;
			}
			len = bytes.length;
			String format = String.format("block:%s,pos:%s,size:%s", blockPos, pos, len);
			System.out.println(format);
			pos += len;
		}
		rout.closeAndReturnBlock();
		lines.close();
		out.close();
	}

	@Test
	public void testWrite() throws IOException {
		GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream("./test.gz"));
		BufferedReader lines = Files.newBufferedReader(new File("./3.log").toPath());
		long offset = 0, i = 0;
		String line = null;
		while ((line = lines.readLine()) != null) {
			line += "\n";
			byte[] bytes = line.getBytes();
			offset += bytes.length;
			gzos.write(bytes);
			System.out.println(i + "--->" + offset);
			i++;
		}
		lines.close();
		gzos.close();
	}

	@Test
	public void testRead() throws FileNotFoundException, IOException {
		GZIPInputStream gz = new GZIPInputStream(new FileInputStream("./test.gz"));
		gz.skip(141845765);
		BufferedReader br = new BufferedReader(new InputStreamReader(gz));
		System.out.println(br.readLine());
		br.close();
		gz.close();
	}

}
