package com.czp.ulc.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.junit.Test;

import com.czp.ulc.common.meta.MyZipOutStream;
import com.czp.ulc.common.meta.StreamIndexing;

/**
 * @dec Function
 * @author coder_czp@126.com
 * @date 2017年5月21日/下午10:34:47
 * @copyright coder_czp@126.com
 *
 */
public class TestZip {

	long posSrc = 0;
	
	@Test
	public void testRead() throws FileNotFoundException, IOException{
		GZIPInputStream gis = new GZIPInputStream(new FileInputStream("./test.zip"));
		byte[] buffer = new byte[209];
		int offset = 14987863;
		gis.skip(offset);
		gis.read(buffer);
		gis.close();
		System.out.println(new String(buffer));
	}
	
//	325286--->14987863---->181
//	325286--->14988072---->209
//	325286--->14988267---->195

	@Test
	public void testWriten() throws FileNotFoundException, IOException {
		MyZipOutStream zos = new MyZipOutStream(new FileOutputStream("test.zip"));
		Stream<String> lines = Files.lines(new File("./2.log").toPath());
		lines.forEach(line -> {
			try {
				byte[] bytes = line.getBytes();
				int len = bytes.length;
				long pos = zos.writeData(bytes);
				zos.write('\n');
				posSrc += len;
				System.out.println(pos + "--->" + posSrc+"---->"+len);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		lines.close();
		zos.close();
	}
}
