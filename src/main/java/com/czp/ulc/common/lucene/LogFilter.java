package com.czp.ulc.common.lucene;

import java.io.IOException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * 对类类路径进行分词
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月1日 下午5:09:32</li>
 * 
 * @version 0.0.1
 */

public class LogFilter extends TokenFilter {

	private CharTermAttribute termAttribute;
	private Pattern p = Pattern.compile("\\[(.*?)\\]");
	//private Pattern clsP = Pattern.compile("([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*([a-zA-Z_$][a-zA-Z\\d_$]*)");
	private LinkedList<String> terms = new LinkedList<String>();

	public LogFilter(TokenStream in) {
		super(in);
		termAttribute = this.addAttribute(CharTermAttribute.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.analysis.TokenStream#incrementToken()
	 */
	@Override
	public final boolean incrementToken() throws IOException {
		if (terms.size() > 0) {
			this.setTermBufferFromList();
			return true;
		} else {
			if (!input.incrementToken()) {
				return false;
			}
			this.splitTerm();
			if (terms.size() > 0) {
				this.setTermBufferFromList();
			}
			return true;
		}
	}

	private void setTermBufferFromList() {
		char[] nextTerm = terms.removeFirst().toCharArray();
		termAttribute.resizeBuffer(nextTerm.length);
		termAttribute.copyBuffer(nextTerm, 0, nextTerm.length);
	}

	private void splitTerm() {
		String term = termAttribute.toString();
		Matcher matcher = p.matcher(term);
		while (matcher.find()) {
			term = matcher.group(1);
			terms.add(term);
		}
	}

}
