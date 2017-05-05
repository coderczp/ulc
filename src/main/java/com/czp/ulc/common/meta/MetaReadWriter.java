package com.czp.ulc.common.meta;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.common.util.Utils;

/**
 * 请添加描述 <li>创建人：Jeff.cao</li> <li>创建时间：2017年5月3日 下午12:40:14</li>
 * 
 * @version 0.0.1
 */

public class MetaReadWriter implements AutoCloseable {

	private String baseDir;
	private long eachFileSize;
	private DataWriter nowWriter;
	private Logger log = LoggerFactory.getLogger(MetaReadWriter.class);
	private MetaCompressManager mcTask = MetaCompressManager.getInstance();

	/** 这些信息会存储到索引,为了节省空间,将名称简化为字符 */
	private static final String FILE_NAME = "f";
	private static final String LINE_NO = "l";
	private static final String LINE_SIZE = "s";
	private static final String LINE_POS = "p";

	public MetaReadWriter(String baseDir, long eachFileSize) throws Exception {
		this.baseDir = baseDir;
		this.eachFileSize = eachFileSize;
		this.nowWriter = getCurrentFileWriter(getCurrentDir(baseDir));
	}

	public MetaReadWriter(String baseDir) throws Exception {
		this(baseDir, 1024 * 1024 * 200);
	}

	private DataWriter getCurrentFileWriter(File baseDir) throws Exception {
		int fileNo = -1;
		File nowFile = null;
		for (File item : baseDir.listFiles()) {
			// 处理mac下的隐藏文件问题
			if (item.isHidden())
				continue;
			boolean isLogFile = item.toString().endsWith(".log");
			if (item.length() >= eachFileSize && isLogFile) {
				mcTask.compressFile(item, true);
			} else if (isLogFile) {
				nowFile = item;
			}
			fileNo = Math.max(fileNo, getFileNumber(item.getName()));
		}
		fileNo++;
		if (nowFile == null) {
			nowFile = new File(baseDir, fileNo + ".log");
		}
		return new DataWriter(nowFile, true);
	}

	/***
	 * 滚动写文件,返回文件的当前行号
	 * 
	 * @param lines
	 * @return
	 * @throws IOException
	 */
	public synchronized String write(List<String> lines) throws Exception {

		if (nowWriter.getFile().length() >= eachFileSize) {
			Utils.close(nowWriter);
			nowWriter = getCurrentFileWriter(getCurrentDir(baseDir));
		}

		int lineSize = 0;
		long lineNo = nowWriter.getlineNo();
		long pointer = nowWriter.getPointer();
		for (String string : lines) {
			String line = string.trim();
			if (line.length() > 0) {
				nowWriter.writeLine(string);
				lineSize++;
			}
		}
		JSONObject json = new JSONObject();
		json.put(LINE_NO, lineNo);
		json.put(LINE_POS, pointer);
		json.put(LINE_SIZE, lineSize);
		json.put(FILE_NAME, nowWriter.getFile());
		return json.toJSONString();
	}

	private Integer getFileNumber(String name) {
		return Integer.valueOf(name.substring(0, name.indexOf(".")));
	}

	private File getCurrentDir(String baseDir) {
		Date day = Utils.igroeHMSTime(System.currentTimeMillis());
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		String fileStr = format.format(day);
		File file = new File(baseDir, fileStr);
		file.mkdirs();
		return file;
	}

	/**
	 * 根据write返回的json信息读取文件
	 * 
	 * @param writeReturnJson
	 * @return
	 */
	public List<String> read(String writeReturnJson) {
		JSONObject json = JSONObject.parseObject(writeReturnJson);
		long lineStart = json.getLongValue(LINE_NO);
		int lineSize = json.getIntValue(LINE_SIZE);
		long linePos = json.getIntValue(LINE_POS);
		String fileStr = json.getString(FILE_NAME);
		File file = new File(fileStr);
		if (file.exists()) {
			return readFromUnCompressFile(file, linePos, lineSize);
		}
		return mcTask.readFromCompressFile(file, lineStart, lineSize);
	}

	private List<String> readFromUnCompressFile(File file, long offset, int size) {
		try {
			long st = System.currentTimeMillis();
			FileInputStream fin = new FileInputStream(file);
			fin.skip(offset);

			String line;
			List<String> lines = new LinkedList<>();
			BufferedReader br = new BufferedReader(new InputStreamReader(fin));
			while ((line = br.readLine()) != null && lines.size() < size) {
				lines.add(line);
			}
			br.close();
			long end = System.currentTimeMillis();
			log.info("read:{} line from:{} time:{}ms", lines.size(), file, end - st);
			return lines;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws Exception {
		Utils.close(nowWriter);
	}

}
