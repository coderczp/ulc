package com.czp.ulc.module.lucene;

import java.io.File;

import org.apache.lucene.analysis.Analyzer;

import com.czp.ulc.util.Utils;

/**
 * Lucene相关配置
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月11日 下午4:41:39</li>
 * 
 * @version 0.0.1
 */

public class LuceneConfig {

	/** 索引文件目录日期格式 */
	public static final String FORMAT = "yyyyMMdd";

	/*** 根目录 */
	public static final File ROOT = new File("./log");

	/** 索引根目录 */
	public static final File INDEX_DIR = new File(ROOT, "index");

	/** 未压缩文件目录 */
	public static final File UNCOMP_DIR = new File(ROOT, "data");

	/**日志分词器*/
	public static final Analyzer ANALYZER = new LogAnalyzer();

	public static final int PARALLEL_SEARCH_THREADS = Utils.getCpus() + 4;

	public static void init() {
		UNCOMP_DIR.mkdirs();
		INDEX_DIR.mkdirs();
	}

}
