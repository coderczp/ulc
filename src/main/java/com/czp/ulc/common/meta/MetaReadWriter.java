package com.czp.ulc.common.meta;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.common.util.Utils;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月3日 下午12:40:14</li>
 * 
 * @version 0.0.1
 */

public class MetaReadWriter implements AutoCloseable {

	public static class LinePos {
		long linePos;
		long size;

		public LinePos(long linePos, long size) {
			this.linePos = linePos;
			this.size = size;
		}

		@Override
		public String toString() {
			return "[linePos=" + linePos + ", size=" + size + "]";
		}

	}

	public static class Meta {
		private int dirId;
		private int fileId;
		private long lineNo;

		public Meta(int dirId, int fileId, long lineNo) {
			this.dirId = dirId;
			this.fileId = fileId;
			this.lineNo = lineNo;
		}

		public int getDirId() {
			return dirId;
		}

		public int getFileId() {
			return fileId;
		}

		public long getLineNo() {
			return lineNo;
		}

		public long getUUID() {
			long dirId = this.dirId;
			return (dirId << 32) | fileId;
		}

	}

	private File baseDir;
	private long eachFileSize;
	private long nowFileLines;
	private DataWriter nowWriter;
	private ExecutorService worker = Executors.newSingleThreadExecutor();

	private static final String SUFIX = ".log";
	private static final String ZIP_SUFIX = ".zip";
	private static final String INDEX_SUFIX = ".index";
	private static final long MAX_SKIP_BUFFER_SIZE = 1024 * 20;
	private static final int DEFAUT_EACH_FILE_SIZE = 1024 * 1024 * 200;

	private static final Logger log = LoggerFactory.getLogger(MetaReadWriter.class);

	public MetaReadWriter(String baseDir) throws Exception {
		this(baseDir, DEFAUT_EACH_FILE_SIZE);
	}

	public MetaReadWriter(String baseDir, int eachFileSize) throws Exception {
		this.baseDir = new File(baseDir);
		this.eachFileSize = eachFileSize;
		this.nowWriter = getTodayWriter(getTodayDir(this.baseDir));
		this.nowFileLines = nowWriter.getlineNo();
		this.checkHasUncompressFile();
	}

	/***
	 * 滚动写文件,返回文件的当前行号
	 * 
	 * @param lines
	 * @return
	 * @throws IOException
	 */
	public synchronized byte[] write(String line) throws Exception {
		if (nowWriter.getFile().length() >= eachFileSize) {
			Utils.close(nowWriter);
			nowWriter = null;
			nowWriter = getTodayWriter(getTodayDir(baseDir));
		}
		int dirId = nowWriter.getDirId();
		int fileId = nowWriter.getFileId();
		long lineNo = nowWriter.getlineNo();
		nowWriter.writeLine(line);
		return encodeMetaId(dirId, fileId, lineNo);
	}

	/***
	 * 4字节目录编号 4字节文件编号 x字节行号(<128:1byte 65536:2byte ....)<br>
	 * 将文件id和目录ID编码为一个long
	 * 
	 * @param dirId
	 * @param fileId
	 * @param lineNo
	 * @return
	 */
	public static byte[] encodeMetaId(int dirId, int fileId, long lineNo) {
		ByteBuffer buf = ByteBuffer.allocate(16);
		buf.putInt(dirId);
		buf.putInt(fileId);
		if (lineNo < Byte.MAX_VALUE) {
			buf.put((byte) lineNo);
		} else if (lineNo < Character.MAX_VALUE) {
			buf.putChar((char) lineNo);
		} else if (lineNo < Integer.MAX_VALUE) {
			buf.putInt((int) lineNo);
		} else {
			buf.putLong(lineNo);
		}
		buf.flip();
		byte[] realArr = new byte[buf.limit()];
		System.arraycopy(buf.array(), 0, realArr, 0, buf.limit());
		return realArr;
	}

	public static Meta decodeMetaId(byte[] metaId) {
		ByteBuffer buf = ByteBuffer.wrap(metaId);
		int dirId = buf.getInt();
		int fileId = buf.getInt();
		int remain = buf.remaining();
		long lineNo = 0;
		if (remain == 1)
			lineNo = buf.get();
		else if (remain == 2)
			lineNo = buf.getChar();
		else if (remain == 4)
			lineNo = buf.getInt();
		else
			lineNo = buf.getLong();
		return new Meta(dirId, fileId, lineNo);
	}

	private String getFileNameFrom(long uuid) {
		int dirId = (int) (uuid >> 32), fileId = (int) uuid;
		return String.format("%s/%s/%s%s", baseDir, dirId, fileId, SUFIX);
	}

	/**
	 * 读取索引压缩文件里的行数
	 * 
	 * @return
	 */
	public long loadLineCount(File dir) {
		long bytes = 0;
		for (File file : dir.listFiles()) {
			if (!file.isDirectory())
				continue;
			for (File item : file.listFiles()) {
				if (!item.getName().endsWith(ZIP_SUFIX) || item.length() == 0)
					continue;
				try (ZipFile zf = new ZipFile(item)) {
					String indexFile = item.getName().replaceAll(ZIP_SUFIX, INDEX_SUFIX);
					bytes += zf.getEntry(indexFile).getSize();
				} catch (Exception e) {
					log.error("read count error:" + item, e);
				}
			}
		}
		long lineCount = nowFileLines + (bytes / Long.BYTES);
		log.info("file:{} linecount:{}", dir, lineCount);
		return lineCount;

	}

	public static void doCompress(File file, File outPut, boolean delSrc) {
		try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outPut)))) {
			long st1 = System.currentTimeMillis();
			long startPos = 0;
			String line = null;
			byte[] lineSpliter = DataWriter.lineSpliter;
			zos.putNextEntry(new ZipEntry(file.getName()));
			LinkedList<Long> linePointer = new LinkedList<>();
			try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
				while ((line = br.readLine()) != null) {
					byte[] bytes = line.getBytes(DataWriter.UTF8);
					startPos += bytes.length + lineSpliter.length;
					linePointer.add(startPos);
					zos.write(bytes);
					zos.write(lineSpliter);
				}
			}
			boolean del = false;
			long size = outPut.length();
			long oldSize = file.length();
			writeLineIndexFile(file, zos, linePointer);
			if (delSrc) {
				del = file.delete();
				File indexFile = getIndexFile(file);
				if (indexFile.exists())
					indexFile.delete();
			}
			long end1 = System.currentTimeMillis();
			log.info("compress[{}],size[{}]->[{}],del[{}],time[{}]ms", file, oldSize, size, del, end1 - st1);
		} catch (Exception e) {
			log.error("fail to compress:" + file, e);
		}
	}

	private DataWriter getTodayWriter(File baseDir) throws Exception {
		int fileNo = -1;
		File nowFile = null;
		for (File item : baseDir.listFiles()) {
			if (!item.getName().endsWith(SUFIX))
				continue;
			if (item.length() >= eachFileSize) {
				ansyComprecessFile(item, true);
			} else {
				nowFile = item;
			}
			fileNo = Math.max(fileNo, getFileId(item));
		}
		fileNo++;
		if (nowFile == null) {
			nowFile = new File(baseDir, fileNo + SUFIX);
		}
		return new DataWriter(nowFile, true);
	}

	private void ansyComprecessFile(File file, boolean delSrc) {
		worker.execute(new Runnable() {
			@Override
			public void run() {
				doCompress(file, getZipFile(file), delSrc);
			}
		});
	}

	private static void writeLineIndexFile(File file, ZipOutputStream zos, LinkedList<Long> index) throws IOException {
		zos.putNextEntry(new ZipEntry(getIndexFile(file).getName()));
		for (Long num : index) {
			zos.write(Utils.longToBytes(num));
		}
	}

	public static File getIndexFile(File file) {
		return new File(file + INDEX_SUFIX);
	}

	private static File getZipFile(File file) {
		return new File(file + ZIP_SUFIX);
	}

	public static int getFileId(File file) {
		String name = file.getName();
		return Integer.parseInt(name.substring(0, name.indexOf(".")));
	}

	private File getTodayDir(File baseDir) {
		Date day = Utils.igroeHMSTime(System.currentTimeMillis());
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		String fileStr = format.format(day);
		File file = new File(baseDir, fileStr);
		file.mkdirs();
		return file;
	}

	private LinePos getLinePos(ZipFile zf, File file, long lineNo) throws IOException {
		if (zf == null) {
			synchronized (this) {
				if (file.equals(nowWriter.getFile())) {
					long pos = nowWriter.getLinePost(lineNo);
					long nexLinePos = nowWriter.getLinePost(lineNo+1);
					return new LinePos(pos, nexLinePos-pos);
				}
			}
			throw new RuntimeException(file + " is not equals nowWriter file:" + nowWriter.getFile());
		}
		ZipEntry indexFile = zf.getEntry(getIndexFile(file).getName());
		return readLinePos(file, zf.getInputStream(indexFile), lineNo);
	}

	// 先读取索引,在根据索引快速定位行对应的文件指针读取行
	private LinePos readLinePos(File file, InputStream is, long lineNo) throws IOException {
		try (InputStream io = is) {
			if (lineNo < 0)
				return new LinePos(0, 0);
			int bytes = Long.BYTES;
			long skip = (lineNo - 1) * bytes;
			byte[] buf = new byte[bytes * 2];
			skipSpecBytes(file, is, skip);
			if (is.read(buf) == -1) {
				throw new IOException("reach end of:" + file);
			}
			ByteBuffer wrap = ByteBuffer.wrap(buf);
			long linePos = wrap.getLong();
			long nextLinePos = wrap.getLong();
			return new LinePos(linePos, nextLinePos - linePos);
		}
	}

	@Override
	public void close() throws Exception {
		Utils.close(nowWriter);
		worker.shutdown();
	}

	public Map<Long, String> readFromLogFile(File file, InputStream is, Set<Meta> lineRequest) throws IOException {
		return readMetaData(lineRequest, file, is, null);
	}

	// 合并读取,避免同一个文件打开关闭多次
	public Map<Long, Map<Long, String>> mergeRead(List<byte[]> lineRequest) throws Exception {
		Map<Long, Map<Long, String>> datas = new HashMap<>();
		Map<Long, Set<Meta>> readLines = classifyByFile(lineRequest);
		for (Entry<Long, Set<Meta>> entry : readLines.entrySet()) {
			long st = System.currentTimeMillis();
			Set<Meta> metas = entry.getValue();
			long uuid = entry.getKey();

			String fileName = getFileNameFrom(uuid);
			File file = new File(fileName);
			Map<Long, String> lines = null;
			String logName = fileName;
			if (file.exists()) {
				lines = readFromLogFile(file, new FileInputStream(file), metas);
			} else {
				lines = readFromZipFile(metas, file);
				logName = getZipFile(file).getName();
			}
			datas.put(uuid, lines);
			long end = System.currentTimeMillis();
			log.info("read[{}]lines,from[{}],time:[{}]ms", metas.size(), logName, end - st);
		}
		return datas;
	}

	private Map<Long, String> readFromZipFile(Set<Meta> lineRequest, File file) throws Exception {
		try (ZipFile zf = new ZipFile(getZipFile(file))) {
			ZipEntry logFile = zf.getEntry(file.getName());
			return readMetaData(lineRequest, file, zf.getInputStream(logFile), zf);
		}
	}

	private Map<Long, String> readMetaData(Set<Meta> metas, File file, InputStream ins, ZipFile zf) throws IOException {
		Map<Long, String> linesMap = new HashMap<>();
		try (InputStream br = ins) {
			long lastSkip = 0, hasReadSize = 0;
			for (Meta item : metas) {
				long lineNo = item.getLineNo();
				LinePos pos = getLinePos(zf, file, lineNo);
				long skip = pos.linePos - lastSkip - hasReadSize;
				long size = pos.size;
				if(skipSpecBytes(file, ins, skip)!=skip){
					System.out.println("=-----------------------------error");
				}
				if (size <= 0) {
					log.error("find index err,file:{},post:{}", file, pos);
					linesMap.put(lineNo, "N/A");
				} else {
					byte[] buf = new byte[(int) size];
					br.read(buf, 0, buf.length);
					String value = new String(buf);
					linesMap.put(lineNo, value);
					hasReadSize += value.length();
					lastSkip = skip;
					//System.out.println(skip+"-->"+lineNo+"-->"+pos+"-->"+value);
				}
			}
		}
		return linesMap;
	}

	// JDK skip 不能正确的跳过指定字节,会导致CPU 100%
	private static long skipSpecBytes(File file, InputStream in, long skip) throws IOException {
		if (skip <= 0)
			return 0;
		long remaining = skip;
		long st = System.currentTimeMillis();
		if (in instanceof FileInputStream) {
			while (remaining > 0) {
				long nr = in.skip(skip);
				if (nr == -1) {
					throw new IOException("reach end of:" + file);
				}
				remaining -= nr;
			}
			return skip;
		}
		int nr = 0;
		int size = (int) Math.min(MAX_SKIP_BUFFER_SIZE, remaining);
		byte[] skipBuffer = new byte[size];
		while (remaining > 0 && nr < 0) {
			nr = in.read(skipBuffer, 0, (int) Math.min(size, remaining));
			if (nr == -1) {
				throw new IOException("reach end of:" + file);
			}
			remaining -= nr;
		}
		long end = System.currentTimeMillis();
		log.info("skip:[{}]bytes,from:[{}],time:[{}]ms", skip, file, end - st);
		return skip;
	}

	// 把读请求按文件分类,这样一个文件只打开一次
	private Map<Long, Set<Meta>> classifyByFile(List<byte[]> metaBytes) {
		Map<Long, Set<Meta>> readLines = new HashMap<>();
		for (byte[] item : metaBytes) {
			Meta meta = decodeMetaId(item);
			long uuid = meta.getUUID();
			Set<Meta> lineNos = readLines.get(uuid);
			if (lineNos == null) {
				lineNos = new TreeSet<>(new Comparator<Meta>() {
					public int compare(Meta o1, Meta o2) {
						return ((Long) o1.getLineNo()).compareTo(o2.getLineNo());
					}
				});
			}
			lineNos.add(meta);
			readLines.put(uuid, lineNos);
		}
		return readLines;
	}

	private void checkHasUncompressFile() {
		File tadayFile = getTodayDir(baseDir);
		for (File file : baseDir.listFiles()) {
			if (!file.exists() || file.getName().equals(tadayFile.getName()))
				continue;
			for (File item : file.listFiles()) {
				if (!item.getName().endsWith(SUFIX))
					continue;
				ansyComprecessFile(item, true);
				log.info("find required file:{},will compress", item);
			}
		}
	}
}
