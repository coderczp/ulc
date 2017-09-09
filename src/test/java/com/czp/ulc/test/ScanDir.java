package com.czp.ulc.test;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ScanDir {

	public static void main(String[] args) throws ParseException {
		File f = new File(args[0]);
		File[] files = f.listFiles();
		SimpleDateFormat sp = new SimpleDateFormat("yyyyMMdd");
		int j = 0;
		for (File file : files) {
			File[] files2 = f.listFiles();
			String server = file.getName();
			for (File file2 : files2) {
				String path = file2.getAbsolutePath();
				long date = sp.parse(file2.getName()).getTime();
				System.out.println(String.format("INSERT INTO `lucene_file` VALUES ('%s', 'pre1', '%s', '%s', '%s');",
						j, server, date, path));
			}
		}
	}
}
