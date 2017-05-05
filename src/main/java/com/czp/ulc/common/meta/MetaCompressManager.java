package com.czp.ulc.common.meta;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.common.ShutdownCallback;
import com.czp.ulc.common.ShutdownManager;
import com.czp.ulc.common.util.Utils;

/**
 * </li> <li>创建时间：2017年5月3日 上午9:20:25</li>
 * 
 * @version 0.0.1
 */

public class MetaCompressManager implements ShutdownCallback {

	private ExecutorService worker = Executors.newSingleThreadExecutor();

	private static MetaCompressManager INSTANCE = new MetaCompressManager();

	private static Logger log = LoggerFactory.getLogger(MetaCompressManager.class);

	private WeakHashMap<String, WeakHashMap<Long, Long>> indexMap = new WeakHashMap<>();

	private MetaCompressManager() {
		ShutdownManager.getInstance().addCallback(this);
	}

	public static MetaCompressManager getInstance() {
		return INSTANCE;
	}

	public void compressFile(File dir, boolean delSrc) {
		worker.execute(new Runnable() {

			@Override
			public void run() {
				try {
					File out = new File(getZipFile(dir));
					doCompress(dir, out, delSrc);
				} catch (Exception e) {
					log.error("exception", e);
				}
			}
		});
	}

	public void doCompress(File file, File outPut, boolean delSrc) {

		ZipOutputStream zos = null;
		try {
			long st1 = System.currentTimeMillis();

			long startPos = 0;
			String line = null;
			byte[] lineSpliter = DataWriter.lineSpliter;
			zos = new ZipOutputStream(new FileOutputStream(outPut));
			zos.putNextEntry(new ZipEntry(file.getName()));
			// 记录每一行开始的指针
			LinkedList<Long> index = new LinkedList<>();
			BufferedReader br = Files.newBufferedReader(file.toPath());
			while ((line = br.readLine()) != null) {
				byte[] bytes = line.getBytes(DataWriter.UTF8);
				startPos += bytes.length + lineSpliter.length;
				index.add(startPos);
				zos.write(bytes);
				zos.write(lineSpliter);
			}
			br.close();
			// 写索引文件
			writeLineIndexFile(file, zos, index);
			long end1 = System.currentTimeMillis();
			log.info("compress file:{} size:{},times:{}", file, file.length(), end1 - st1);
			if (delSrc) {
				boolean b = file.delete();
				log.info("delete:{} {},after compress", file, b);
			}
		} catch (Exception e) {
			log.error("compress error", e);
		} finally {
			Utils.close(zos);
		}
	}

	private void writeLineIndexFile(File file, ZipOutputStream zos, LinkedList<Long> index) throws IOException {
		zos.putNextEntry(new ZipEntry(getIndexFileName(file)));
		for (Long num : index) {
			zos.write(Utils.longToBytes(num));
		}
	}

	private String getIndexFileName(File file) {
		return file.getName().concat(".index");
	}

	private String getZipFile(File file) {
		return file + ".zip";
	}

	public List<String> readFromCompressFile(File file, long lineStart, int size) {

		ZipFile zf = null;
		try {
			long st = System.currentTimeMillis();

			LinkedList<String> lines = new LinkedList<>();
			String zipFile = getZipFile(file);
			String tmp;

			zf = new ZipFile(zipFile);
			ZipEntry entry = zf.getEntry(file.getName());
			long linePointer = getLinePos(zf, file, lineStart);
			if (linePointer == -1) {
				log.error("maybe indexfile is bad");
				return lines;
			}
			
			InputStream inputStream = zf.getInputStream(entry);
			inputStream.skip(linePointer);
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			while ((tmp = br.readLine()) != null && lines.size() < size) {
				lines.add(tmp);
			}
			br.close();
			zf.close();
			long end = System.currentTimeMillis();
			log.info("read:{}lines,from:{} time:{}", size, zipFile, end - st);
			return lines;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			Utils.close(zf);
		}
	}

	// 先读取索引,在根据索引快速定位行对应的文件指针读取行
	private long getLinePos(ZipFile zf, File file, long lineNo) throws IOException {
		if (lineNo == 0)
			return 0;
		String key = file.toString();
		WeakHashMap<Long, Long> lineIndexMap = indexMap.get(key);
		if (lineIndexMap == null) {
			synchronized (file) {
				loadAllLineIndex(zf, file);
			}
		}
		Long pos = indexMap.get(key).get(lineNo);
		return pos == null ? -1 : pos;
	}

	private void loadAllLineIndex(ZipFile zf, File file) {
		long pos = -1;
		long line = 0;
		ZipEntry indexFile = zf.getEntry(getIndexFileName(file));
		WeakHashMap<Long, Long> lineIndexMap = new WeakHashMap<>();
		try (DataInputStream dis = new DataInputStream(zf.getInputStream(indexFile))) {
			while ((pos = dis.readLong()) != -1) {
				lineIndexMap.put(line++, pos);
			}
		} catch (Throwable e) {
			log.error("load line index error:"+file, e);
		}
		if (lineIndexMap.size() > 0)
			indexMap.put(file.toString(), lineIndexMap);
	}

	@Override
	public void onSystemExit() {
		worker.shutdown();
	}

}
