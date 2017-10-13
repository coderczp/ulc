package com.czp.ulc.module.lucene;

import java.io.File;

import org.apache.lucene.analysis.Analyzer;
import org.springframework.core.env.Environment;

import com.czp.ulc.util.Utils;

/**
 * Lucene相关配置
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月11日 下午4:41:39</li>
 * 
 * @version 0.0.1
 */

public class LuceneConfig {

	/*** 根目录 */
	private static File root;

	/** 索引根目录 */
	private static File indexDir;

	/** 待索引的文件目录 */
	private static File dataDir;

	/** 索引文件目录日期格式 */
	public static final String FORMAT = "yyyyMMdd";

	/** 日志分词器 */
	public static final Analyzer ANALYZER = new LogAnalyzer();

	public static final int PARALLEL_SEARCH_THREADS = Utils.getCpus() + 4;

	public static void config(Environment env) {
		String rootPath = env.getProperty("index.root.path");
		root = new File(rootPath);
		dataDir = new File(root, "data");
		indexDir = new File(root, "index");
		dataDir.mkdirs();
		indexDir.mkdirs();
	}

	public static File getRoot() {
		return root;
	}

	public static void setRoot(File root) {
		LuceneConfig.root = root;
	}

	public static File getIndexDir() {
		return indexDir;
	}

	public static void setIndexDir(File indexDir) {
		LuceneConfig.indexDir = indexDir;
	}

	public static File getDataDir() {
		return dataDir;
	}

	public static void setDataDir(File dataDir) {
		LuceneConfig.dataDir = dataDir;
	}

}
