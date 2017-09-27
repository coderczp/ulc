package com.czp.ulc.module.lucene;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.util.IOUtils;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月1日 下午3:55:48</li>
 * 
 * @version 0.0.1
 */

public class LogAnalyzer extends Analyzer {

	private CharArraySet stopWords;

	public LogAnalyzer(CharArraySet stopWords) {
		this.stopWords = stopWords;
	}

	public LogAnalyzer(Reader stopwords) throws IOException {
		try {
			stopWords = WordlistLoader.getWordSet(stopwords);
		} finally {
			IOUtils.close(stopwords);
		}
	}

	public LogAnalyzer() {
		stopWords = new CharArraySet(Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if",
				"in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then",
				"there", "these", "they", "this", "to", "was", "will", "with", "null", "--", "args", "api", "info",
				"warn", "error", "debug", ".", "method", "ms.", "rpc", "info:", "ms", "spendtime", "http:", "spend",
				"clientip", "clientport","error:", ":", "dubbo", "client", "invoke", "serverip","serverport"), false);
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		LogTokenizer source = new LogTokenizer();
		TokenStream tok = new StopFilter(source, stopWords);
		return new TokenStreamComponents(source, new LogTokenFilter(tok));
	}

}
