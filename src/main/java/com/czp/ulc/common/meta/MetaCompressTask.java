package com.czp.ulc.common.meta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.common.ShutdownCallback;
import com.czp.ulc.common.ShutdownManager;
import com.czp.ulc.common.ThreadPools;
import com.czp.ulc.common.util.Utils;

/**
 * 在编程设计中，Glob是一种模式，它使用通配符来指定文件名。例如：.Java就是一个简单的Glob，它指定了所有扩展名为“java”的文件。<br>
 * Glob模式中广泛使用了两个通配符“”和“?”。其中星号表示“任意的字符或字符组成字符串”，而问号则表示“任意单个字符”。
 * Glob模式源于Unix操作系统，Unix提供了一个“global命令”,它可以缩写为glob。Glob模式与正则表达式类似，但它的功能有限。 Java
 * SE7的NIO库中引入了Glob模式，它用于FileSystem类，在PathMatcher getPathMatcher(String
 * syntaxAndPattern)方法中使用。Glob可以作为参数传递给PathMatcher。同样地，在Files类中也可以使用Glob来遍历整个目录。
 * 下面是Java NIO中使用的Glob模式描述：<br>
 * .txt 匹配所有扩展名为.txt的文件<br>
 * .{html,htm} 匹配所有扩展名为.html或.htm的文件<br>
 * { }用于组模式，它使用逗号分隔 ?<br>
 * .txt 匹配任何单个字符做文件名且扩展名为.txt的文件 <br>
 * . 匹配所有含扩展名的文件 C:\Users\* 匹配所有在C盘Users目录下的文件<br>
 * 反斜线“\”用于对紧跟的字符进行转义 /home/** UNIX平台上匹配所有/home目录及子目录下的文件<br>
 * **用于匹配当前目录及其所有子目录 [xyz].txt<br>
 * 匹配所有单个字符作为文件名，且单个字符只含“x”或“y”或“z”三种之一，且扩展名为.txt的文件。方括号[]用于指定一个集合 [a-c].txt
 * 匹配所有单个字符作为文件名，且单个字符只含“a”或“b”或“c”三种之一，且扩展名为.txt的文件。减号“-”用于指定一个范围，且只能用在方括号[]内
 * [!a].txt 匹配所有单个字符作为文件名，且单个字符不能包含字母“a”，且扩展名为.txt的文件。叹号“!”用于否定
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月3日 上午9:20:25</li>
 * 
 * @version 0.0.1
 */

public class MetaCompressTask implements Runnable, ShutdownCallback {

	private boolean delSrcDir;

	private static MetaCompressTask INSTANCE;

	private File EXIT = new File("_task_exit_");

	private Logger log = LoggerFactory.getLogger(MetaCompressTask.class);

	private LinkedBlockingQueue<File> waitCompress = new LinkedBlockingQueue<>();

	private MetaCompressTask(boolean delSrcDir) {
		this.delSrcDir = delSrcDir;
		ThreadPools.getInstance().startThread("compress-task", this, true);
		ShutdownManager.getInstance().addCallback(this);
	}

	public static MetaCompressTask getInstance(boolean delSrcDir) {
		if (INSTANCE == null) {
			synchronized (MetaCompressTask.class) {
				if (INSTANCE == null) {
					INSTANCE = new MetaCompressTask(delSrcDir);
				}
			}
		}
		return INSTANCE;
	}

	public void add(File dir) {
		waitCompress.add(dir);
	}

	@Override
	public void run() {

		ZipOutputStream zos = null;
		LinkedList<File> files = new LinkedList<>();
		SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
				files.add(path.toFile());
				return super.visitFile(path, attrs);
			}

		};
		while (!Thread.interrupted()) {
			try {
				File dir = waitCompress.take();
				if (dir == EXIT)
					break;

				long st0 = System.currentTimeMillis();
				try {
					File zip = new File(dir.getParentFile(), dir.getName() + ".zip");
					zos = new ZipOutputStream(new FileOutputStream(zip));
					Files.walkFileTree(dir.toPath(), visitor);
					String dirStr = dir.toString();
					int len = dirStr.length() + 1;
					while (!files.isEmpty()) {
						File file = files.removeFirst();
						String string = file.toString();
						String name = string.substring(string.indexOf(dirStr) + len);
						String zipName = new File(name).toString();
						long st1 = System.currentTimeMillis();
						zos.putNextEntry(new ZipEntry(zipName));
						Files.copy(file.toPath(), zos);
						long end1 = System.currentTimeMillis();
						log.info("compress file:{} size:{},times:{}", file, file.length(), end1 - st1);
						if (delSrcDir) {
							boolean b = file.delete();
							log.info("delete:{} {},after compress", file, b);
						}
					}
				} catch (Exception e) {
					log.error("compress error", e);
				} finally {
					Utils.close(zos);
				}
				if (delSrcDir) {
					boolean delete = dir.delete();
					log.info("delete:{} {}", dir, delete);
				}
				long end0 = System.currentTimeMillis();
				log.info("compress dir:{} times:{}", dir, end0 - st0);
			} catch (InterruptedException e) {
				log.error("InterruptedException", e);
			}
		}
	}

	@Override
	public void onSystemExit() {
		waitCompress.add(EXIT);
	}

}
