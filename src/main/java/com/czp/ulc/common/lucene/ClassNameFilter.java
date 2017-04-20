package com.czp.ulc.common.lucene;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

/**
 * 对类类路径进行分词
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月1日 下午5:09:32</li>
 * 
 * @version 0.0.1
 */

public class ClassNameFilter extends TokenFilter {

	private CharTermAttribute termAttribute;
	private PositionIncrementAttribute positionIncrementAttribute;

	private LinkedList<String> terms = new LinkedList<String>();

	public ClassNameFilter(TokenStream in) {
		super(in);
		termAttribute = this.addAttribute(CharTermAttribute.class);
		positionIncrementAttribute = this.addAttribute(PositionIncrementAttribute.class);
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
		positionIncrementAttribute.setPositionIncrement(0);
	}

	private void splitTerm() {
		String term = termAttribute.toString();
		String[] t = term.split("\\.");
		if (t.length > 1) {
			terms = new LinkedList<String>();
			for (String s : t) {
				terms.add(s);
			}
		}
	}

}
