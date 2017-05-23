package com.czp.ulc.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
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
		long st =System.currentTimeMillis();
		TreeMap<Integer, Integer> lineOffset = new TreeMap<>();
		lineOffset.put(19487906, 107);
		lineOffset.put(19488559, 83);
		lineOffset.put(19490153, 63);
		lineOffset.put(19490433, 247);
		
		RACFileReader r = new RACFileReader(new File("./log/zip/0.rac"));
		TreeMap<Integer, byte[]> bos = r.readLines(0,lineOffset);
		Set<Entry<Integer, byte[]>> entrySet = bos.entrySet();
		for (Entry<Integer, byte[]> entry : entrySet) {
			System.out.println(new String(entry.getValue()));
		}
		r.close();
		System.out.println(System.currentTimeMillis()-st);
	}

	@Test
	public void testWrite2() throws IOException {
		long st =System.currentTimeMillis();
		String name = "./test.rac";
		File file = new File("./3.log");
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
			//System.out.println(String.format("block:%s,pos:%s,size:%s", blockPos, pos, len));
			pos += len;
		}
		//System.out.println(file.length());
		rout.closeAndReturnBlock();
		lines.close();
		out.close();
		System.out.println("compress:"+file.length()+",time:"+(System.currentTimeMillis()-st));
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
