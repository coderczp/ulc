package com.czp.ulc.test;

import java.io.File;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月30日</li>
 * 
 * @version 0.0.1
 */

public class SpaceTest {

	public static void main(String[] args) {
		File file = new File("/");
		long totalSpace = file.getTotalSpace();
		System.out.println("C盘空间大小：" + totalSpace / 1024f / 1024 / 1024 + "G");
		long freeSpace = file.getFreeSpace();
		System.out.println("C盘剩余空间大小：" + freeSpace / 1024f / 1024 / 1024 + "G");
	}
}
