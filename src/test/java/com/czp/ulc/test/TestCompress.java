package com.czp.ulc.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

import com.czp.ulc.common.meta.MemOutStream;
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
		MemOutStream bos = new MemOutStream(15014389);
		RACFileReader r = new RACFileReader(new File("./test.rac"));
		r.readBlock(0, bos);
		r.close();
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.getBuf(),0,bos.size());
		 bis.skip(14988072);
		byte[] x = new byte[195];
		bis.read(x,0,x.length);
		System.out.println(new String(x));
	}
//	block:0,pos:14987863,size:209
//	block:0,pos:14988072,size:195
//	block:0,pos:14988267,size:175
//	block:0,pos:14988442,size:238
//	block:0,pos:14988680,size:243
//	block:0,pos:14988923,size:187
//	block:0,pos:14989110,size:214
//	block:0,pos:14989324,size:183
//	block:0,pos:14989507,size:182

	@Test
	public void testWrite2() throws IOException {
		String name = "./test.rac";
		File file = new File("./2.log");
		File f = new File(name);
		FileOutputStream out = new FileOutputStream(f);
		BufferedReader lines = Files.newBufferedReader(file.toPath());
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
		System.out.println(file.length());
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
