package com.czp.ulc.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.junit.Test;

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
}
