package com.czp.ulc.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.store.FSDirectory;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月27日 下午2:19:42</li>
 * 
 * @version 0.0.1
 */

public class Lucenetest {

	public static void main(String[] args) throws IOException {
		Analyzer analyzer = new StandardAnalyzer();
		LogByteSizeMergePolicy mergePolicy = new LogByteSizeMergePolicy();
		mergePolicy.setMergeFactor(50000);

		IndexWriterConfig conf = new IndexWriterConfig(analyzer);
		conf.setOpenMode(OpenMode.CREATE_OR_APPEND);
		conf.setMergePolicy(mergePolicy);
		conf.setUseCompoundFile(false);

		File log = new File("./b.log");
		File file = new File("./new_log");
		long st = System.currentTimeMillis();
		IndexWriter writer = new IndexWriter(FSDirectory.open(file.toPath()), conf);
		InputStream stream = Files.newInputStream(log.toPath());
		InputStreamReader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
		Document doc = new Document();
		doc.add(new TextField("contents", new BufferedReader(in)));
		writer.addDocument(doc);
		stream.close();
		writer.close();
		long end = System.currentTimeMillis();
		System.out.println(end-st);
	}
}
