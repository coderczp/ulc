package com.czp.ulc.common.module.lucene;

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
			TokenStream stream = analyzer.tokenStream("log", new StringReader(str));
			CharTermAttribute cta = stream.addAttribute(CharTermAttribute.class);
			stream.reset();
			while (stream.incrementToken()) {
				System.out.print("["+cta+"]");
			}
			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
