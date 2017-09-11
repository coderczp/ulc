package com.czp.ulc.common.module.lucene;

import org.apache.lucene.analysis.util.CharTokenizer;

/**
 * @dec Function
 * @author coder_czp@126.com
 * @date 2017年5月15日/下午10:51:30
 * @copyright coder_czp@126.com
 *
 */
public class LowerWhitespaceTokenizer extends CharTokenizer {

	@Override
	protected boolean isTokenChar(int ch) {
		return !Character.isWhitespace(ch) && ch != '@' && ch != '[' && ch != ']' && ch != ',' && ch != '{'
				&& ch != '}' && ch != '/';
	}

	@Override
	protected int normalize(int c) {
		return Character.toLowerCase(c);
	}

}
