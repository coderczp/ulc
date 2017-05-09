package com.czp.ulc.common.lucene;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月1日 下午3:55:48</li>
 * 
 * @version 0.0.1
 */

public class MyAnalyzer extends StopwordAnalyzerBase {

	/**
	 * An unmodifiable set containing some common English words that are not
	 * usually useful for searching.
	 */
	public static final CharArraySet ENGLISH_STOP_WORDS_SET;

	static {
		final List<String> stopWords = Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by", "for",
				"if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then",
				"there", "these", "they", "this", "to", "was", "will", "with", "null", "class","[","]");
		final CharArraySet stopSet = new CharArraySet(stopWords, false);
		ENGLISH_STOP_WORDS_SET = CharArraySet.unmodifiableSet(stopSet);
	}

	/** Default maximum allowed token length */
	public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;

	private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;

	/**
	 * An unmodifiable set containing some common English words that are usually
	 * not useful for searching.
	 */
	public static final CharArraySet STOP_WORDS_SET = ENGLISH_STOP_WORDS_SET;

	/**
	 * Builds an analyzer with the given stop words.
	 * 
	 * @param stopWords
	 *            stop words
	 */
	public MyAnalyzer(CharArraySet stopWords) {
		super(stopWords);
	}

	/**
	 * Builds an analyzer with the default stop words ({@link #STOP_WORDS_SET}).
	 */
	public MyAnalyzer() {
		this(STOP_WORDS_SET);
	}

	/**
	 * Builds an analyzer with the stop words from the given reader.
	 * 
	 * @see WordlistLoader#getWordSet(Reader)
	 * @param stopwords
	 *            Reader to read stop words from
	 */
	public MyAnalyzer(Reader stopwords) throws IOException {
		this(loadStopwordSet(stopwords));
	}

	/**
	 * Set maximum allowed token length. If a token is seen that exceeds this
	 * length then it is discarded. This setting only takes effect the next time
	 * tokenStream or tokenStream is called.
	 */
	public void setMaxTokenLength(int length) {
		maxTokenLength = length;
	}

	/**
	 * Returns the current maximum token length
	 * 
	 * @see #setMaxTokenLength
	 */
	public int getMaxTokenLength() {
		return maxTokenLength;
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		final WhitespaceTokenizer src = new WhitespaceTokenizer();
		TokenStream tok = new StopFilter(src, stopwords);
		tok = new LogFilter(tok);
		tok = new LowerCaseFilter(tok);
		return new TokenStreamComponents(src, tok);
	}

	@Override
	protected TokenStream normalize(String fieldName, TokenStream in) {
		TokenStream result = new StandardFilter(in);
		result = new LowerCaseFilter(result);
		return result;
	}
}
