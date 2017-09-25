package com.czp.ulc.module.lucene;

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
	public static String displayToken(String str, Analyzer analyzer) {
		try {
			TokenStream stream = analyzer.tokenStream("f", new StringReader(str));
			CharTermAttribute cta = stream.addAttribute(CharTermAttribute.class);
			StringBuilder info = new StringBuilder();
			stream.reset();
			while (stream.incrementToken()) {
				info.append("[").append(cta).append("]");
			}
			return info.toString();
		} catch (IOException e) {
			return e.toString();
		}
	}
}
