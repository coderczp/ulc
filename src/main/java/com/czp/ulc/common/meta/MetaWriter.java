package com.czp.ulc.common.meta;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.common.util.Utils;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月3日 下午12:40:14</li>
 * 
 * @version 0.0.1
 */

public class MetaWriter implements AutoCloseable {

	private String baseDir;
	private File currentFile;
	private File lastDayDir;
	private BufferedWriter bw;
	private long eachFileSize;
	private Charset UTF8 = Charset.forName("UTF-8");
	private AtomicInteger linePointer = new AtomicInteger();
	private Logger log = LoggerFactory.getLogger(MetaWriter.class);
	private MetaCompressTask mcTask = MetaCompressTask.getInstance(true);
	private ConcurrentHashMap<String, JSONObject> map = new ConcurrentHashMap<>();

	public MetaWriter(String baseDir, long eachFileSize) {
		this.baseDir = baseDir;
		this.eachFileSize = eachFileSize;
		checkHasUnCompressFile(baseDir);
	}

	private void checkHasUnCompressFile(String baseDir) {
		File f = new File(baseDir);
		File file = getCurrentFile();
		File[] listFiles = f.listFiles();
		for (File item : listFiles) {
			if (!item.getName().endsWith(".zip") && !item.equals(file)) {
				mcTask.add(item);
			}
		}
	}

	public MetaWriter(String baseDir) {
		this(baseDir, 1024 * 1024 * 500);
	}

	/***
	 * 滚动写文件,返回文件的当前行号
	 * 
	 * @param lines
	 * @return
	 * @throws IOException
	 */
	public String write(List<String> lines) throws IOException {
		checkFile();
		int lineNo = linePointer.get();
		for (String string : lines) {
			String line = string.trim();
			if (line.length() > 0) {
				bw.write(string);
				bw.newLine();
			}
		}
		linePointer.getAndAdd(lines.size());
		JSONObject json = new JSONObject();
		json.put("line", lineNo);
		json.put("file", currentFile);
		return json.toJSONString();
	}

	private synchronized void checkFile() {
		try {
			File file = getCurrentFile();
			String string = file.toString();
			JSONObject json = map.get(string);
			if (json != null) {
				currentFile = new File(json.getString("file"));
				bw = (BufferedWriter) json.get("writer");
			} else {
				// 找到最大文件编号
				int no = 0;
				if (!file.exists()) {
					// 当前目录为空,上一个目录需要压缩
					file.mkdirs();
					if (lastDayDir != null) {
						bw.close();
						mcTask.add(lastDayDir);
					}
					lastDayDir = file;
				}
				File[] listFiles = file.listFiles();
				for (File item : listFiles) {
					String name = item.getName();
					int fileNo = Integer.valueOf(name.substring(0, name.lastIndexOf(".")));
					if (fileNo >= no) {
						currentFile = item;
						no = fileNo;
					}
				}

				if (listFiles.length == 0)
					currentFile = new File(file, no + ".log");

				bw = new BufferedWriter(new FileWriter(currentFile));
				json = new JSONObject();
				json.put("file", currentFile);
				json.put("writer", bw);
				map.put(string, json);
			}

			if (currentFile.length() >= eachFileSize) {
				// 文件已经达到临界点,切换文件
				Utils.close(bw);
				int fileNo = Integer.valueOf(currentFile.getName()) + 1;
				currentFile = new File(file, fileNo + ".log");
				bw = new BufferedWriter(new FileWriter(currentFile));
				json.put("file", currentFile);
				json.put("writer", bw);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private File getCurrentFile() {
		Date day = Utils.igroeHMSTime(System.currentTimeMillis());
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		String fileStr = format.format(day);
		File file = new File(baseDir, fileStr);
		return file;
	}

	/**
	 * 根据write返回的json信息读取文件
	 * 
	 * @param writeReturnJson
	 * @return
	 */
	public List<String> read(String writeReturnJson, int size) {
		JSONObject json = JSONObject.parseObject(writeReturnJson);
		String fileStr = json.getString("file");
		int line = json.getIntValue("line");
		File file = new File(fileStr);
		if (file.exists()) {
			return readFromUnCompressFile(file, line, size);
		}
		return readFromCompressFile(file, line, size);
	}

	private List<String> readFromCompressFile(File file, int startLine, int size) {
		try {
			String tmp;
			int lineNo = 0;
			File pFile = file.getParentFile();
			long st = System.currentTimeMillis();
			LinkedList<String> lines = new LinkedList<>();
			ZipFile zf = new ZipFile(new File(pFile.getParentFile(), pFile.getName() + ".zip"));
			ZipEntry entry = zf.getEntry(file.getName());
			BufferedReader br = new BufferedReader(new InputStreamReader(zf.getInputStream(entry)));
			while ((tmp = br.readLine()) != null) {
				if (lineNo++ >= startLine) {
					lines.add(tmp);
				}
				if (lines.size() > size)
					break;
			}
			br.close();
			zf.close();
			long end = System.currentTimeMillis();
			log.info("read:{} lines, from:{} time:{} lineNo:{}", size, startLine, end - st, lineNo);
			return lines;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private List<String> readFromUnCompressFile(File file, int startLine, int size) {
		try {
			return Files.readLines(file, UTF8, new LineProcessor<List<String>>() {

				LinkedList<String> lines = new LinkedList<>();

				int lineNo;

				@Override
				public boolean processLine(String line) throws IOException {
					if (lineNo++ >= startLine) {
						lines.add(line);
					}
					return lines.size() <= size;
				}

				@Override
				public List<String> getResult() {
					return lines;
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws Exception {
		if (bw != null)
			bw.close();
	}

}
