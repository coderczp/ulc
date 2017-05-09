package com.czp.ulc.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;

import com.czp.ulc.common.lucene.AnalyzerUtil;
import com.czp.ulc.common.lucene.MyAnalyzer;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月27日 下午2:19:42</li>
 * 
 * @version 0.0.1
 */

public class Lucenetest {

	public static void main(String[] args) throws IOException {
		 String path = "./log/data/20170509/0.log";
		 BufferedReader br = new BufferedReader(new FileReader(new
		 File(path)));
		 String temp = "";
		 int line = 50;
		 while (line-- > 0 && (temp = br.readLine()) != null) {
		 Analyzer analyzer = new MyAnalyzer();
		 AnalyzerUtil.displayToken(temp, analyzer);
		 analyzer.close();
		 }
		 br.close();
		// Analyzer analyzer2 = new StopAnalyzer(Version.LUCENE_40);
		// Analyzer analyzer3 = new SimpleAnalyzer(Version.LUCENE_40);
		// Analyzer analyzer4 = new WhitespaceAnalyzer(Version.LUCENE_40);

//		Pattern p = Pattern.compile("([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*([a-zA-Z_$][a-zA-Z\\d_$]*)");
//		Matcher matcher = p.matcher("com.czp.ulc.collect.RemoteLogCollector.java:48");
//		while (matcher.find()) {
//			System.out.println(matcher.group(matcher.groupCount()));
//		}
	}
}
