package com.czp.ulc.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.Test;

/**
 * @dec Function
 * @author coder_czp@126.com
 * @date 2017年5月13日/下午6:23:12
 * @copyright coder_czp@126.com
 *
 */
public class TestCompress {

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
	public void testRead() throws FileNotFoundException, IOException{
		GZIPInputStream gz = new GZIPInputStream(new FileInputStream("./test.gz"));
		gz.skip(215960);
		BufferedReader br = new BufferedReader(new InputStreamReader(gz));
		System.out.println(br.readLine());
		br.close();
		gz.close();
	}
//	256--->215960
//	257--->216142
//	258--->216355
//	259--->216627
//	260--->216854
//	261--->217020
//	262--->217216
//	263--->217426
//	264--->217641
//	265--->217880
//	266--->218124
//	267--->218300
//	268--->218488
//	269--->218672
}
