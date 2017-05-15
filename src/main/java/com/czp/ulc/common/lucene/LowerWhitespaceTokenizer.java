package com.czp.ulc.common.lucene;

import org.apache.lucene.analysis.util.CharTokenizer;

/**
 * 在分析完成字符小写转化,避免后续在转化
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月15日 下午1:51:31</li>
 * 
 * @version 0.0.1
 */

public class LowerWhitespaceTokenizer extends CharTokenizer {

	@Override
	protected boolean isTokenChar(int c) {
		return !Character.isWhitespace(c) && c != '<' && c != '>' && c != '[' && c != ']' && c != ',' && c != '{'
				&& c != '}' && c != '"' && c != '/' && c != '\\' && c != '_';
	}

	@Override
	protected int normalize(int c) {
		return Character.toLowerCase(c);
	}

}
