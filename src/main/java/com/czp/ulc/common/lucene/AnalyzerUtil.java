package com.czp.ulc.common.lucene;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月1日 下午4:14:55</li>
 * 
 * @version 0.0.1
 */

public class AnalyzerUtil {
	/**
	 *
	 * Description: 查看分词信息
	 * 
	 * @param str
	 *            待分词的字符串
	 * @param analyzer
	 *            分词器
	 *
	 */
	public static void displayToken(String str, Analyzer analyzer) {
		try {
			// 将一个字符串创建成Token流
			TokenStream stream = analyzer.tokenStream("log", new StringReader(str));
			// 保存相应词汇
			CharTermAttribute cta = stream.addAttribute(CharTermAttribute.class);
			stream.reset();
			while (stream.incrementToken()) {
				System.out.println(cta);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//	public static void main(String[] args) {
//		 Analyzer aly1 = new MyAnalyzer();
//		 String str = "2017-04-01 16:45:44.760 INFO 9764 --- [nio-9123-exec-6]  java.lang.NullPointerException";
//		 AnalyzerUtil.displayToken(str, aly1);
//	}
}
