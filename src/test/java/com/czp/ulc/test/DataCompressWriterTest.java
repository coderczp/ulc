package com.czp.ulc.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.czp.ulc.common.meta.MetaCompressTask;
import com.czp.ulc.common.meta.MetaWriter;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月3日 上午9:56:01</li>
 * 
 * @version 0.0.1
 */

public class DataCompressWriterTest {

	private File dir = new File("D:/WorkSpaces/Eclipse4.4_AOLIDAY_sendy2/service_booking/booking");

	@Test
	public void testCompress() {
		MetaCompressTask dt = MetaCompressTask.getInstance(false);
		dt.add(dir);
		dt.onSystemExit();
	}

	@Test
	public void testWriter() throws Exception {
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");
		LinkedList<File> files = new LinkedList<>();
		SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
				if (matcher.matches(path)) {
					files.add(path.toFile());
				}
				return super.visitFile(path, attrs);
			}

		};
		Files.walkFileTree(dir.toPath(), visitor);
		MetaWriter mw = new MetaWriter("./meta_log");
		for (File file : files) {
			List<String> lines = Files.readAllLines(file.toPath());
			String write = mw.write(lines);
			System.out.println(write);
		}
		mw.close();
		System.out.println("press any key to quit");
		System.in.read();
	}
}
