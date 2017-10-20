package com.czp.ulc.module.lucene.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * 基于文件的DB,负责写入和读取LOG数据
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月20日</li>
 * 
 * @version 0.0.1
 */

public class ZipFileDB {

	private File path;
	private GZIPOutputStream zout;

	public ZipFileDB(File path) throws FileNotFoundException, IOException {
		this.path = path;
		this.zout = new GZIPOutputStream(new FileOutputStream(path));
	}

	public long write(byte[] data) throws IOException {
		zout.write(data);
		return 12;
	}
}
