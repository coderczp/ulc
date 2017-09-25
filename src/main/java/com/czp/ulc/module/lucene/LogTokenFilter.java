package com.czp.ulc.module.lucene;

import java.io.IOException;
import java.util.Stack;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;

/**
 * 对日志格式进行处理
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月1日 下午5:09:32</li>
 * 
 * @version 0.0.1
 */

public class LogTokenFilter extends TokenFilter {

	private char[] rid = "rid:".toCharArray();
	private char[] host = "host:".toCharArray();
	private char[] java = ".java".toCharArray();
	private char[] exception = "exception".toCharArray();

	private AttributeSource.State current;
	private Stack<char[]> remain = new Stack<char[]>();
	private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
	private final PositionIncrementAttribute pos = addAttribute(PositionIncrementAttribute.class);

	protected LogTokenFilter(TokenStream input) {
		super(input);
	}

	@Override
	public boolean incrementToken() throws IOException {

		if (!remain.isEmpty()) {
			char[] syn = remain.pop();
			restoreState(current);
			termAttr.copyBuffer(syn, 0, syn.length);
			pos.setPositionIncrement(0); // Set position increment to 0
			return true;
		}

		if (!this.input.incrementToken())
			return false;

		char[] buffer = termAttr.buffer();
		int len = termAttr.length();
		int start = 0;
		int end = len;

		// 处理orderid=320232635
		if (processKeyVal(buffer, start, end)) {
			current = captureState();
			return true;
		}

		// 移除前后的非数字或字符如: :1378#->1378
		if (!Character.isLetterOrDigit(buffer[0])) {
			start++;
		}
		char lastChar = buffer[end - 1];
		if (!Character.isLetterOrDigit(lastChar) && lastChar != '>') {
			end--;
		}

		// 检测是否是字符串并且包含.java:xx
		int javaIndex = endOf(buffer, start, end, java);
		if (javaIndex != -1) {
			end = javaIndex;
		}
		// 处理rid:xxx ->xxx
		int startIndex = startWith(buffer, start, end, rid);
		if (startIndex > 0) {
			start = startIndex + 1;
		}

		int hoststartIndex = startWith(buffer, start, end, host);
		if (hoststartIndex > 0) {
			start = hoststartIndex + 1;
		}

		// 将com.alibaba.fastjson.jsonexception处理为jsonexception
		int exeIndex = endWith(buffer, start, end, exception);
		if (exeIndex != -1) {
			while (exeIndex >= start && buffer[exeIndex] != '.')
				exeIndex--;
			start = exeIndex + 1;
		}

		int realLen = end - start;
		if (realLen > 0 && (start != 0 || end != len)) {
			clearAttributes();
			termAttr.resizeBuffer(realLen);
			termAttr.copyBuffer(buffer, start, realLen);
			return true;
		}
		return true;
	}

	// 将 orderid=320232635转化为 [orderid=320232635,320232635]
	private boolean processKeyVal(char[] buffer, int start, int end) {
		for (int i = start; i < end; i++) {
			char c = buffer[i];
			if (c == '=') {
				int size_val = end - i - 1;
				char[] value = new char[size_val];
				System.arraycopy(buffer, i + 1, value, 0, size_val);
				remain.push(value);
				return true;
			}
		}
		return false;
	}

	private int endWith(char[] buffer, int start, int end, char[] target) {
		int length = target.length;
		int k = end - 1;
		for (int j = length - 1; j >= 0; j--) {
			if (k < start || target[j] != buffer[k--]) {
				return -1;
			}
		}
		return k;
	}

	private int startWith(char[] buffer, int start, int end, char[] target) {
		int len = Math.min(target.length, Math.min(buffer.length, end));
		if (len == 0)
			return -1;

		int k = 0;
		for (; k < len; k++) {
			if (target[k] != buffer[k + start])
				return -1;
		}
		return k;
	}

	private int endOf(char[] buffer, int start, int end, char[] target) {
		int length = target.length;
		for (int i = end; i >= start; i--) {
			if (buffer[i] == ':') {
				int k = i - 1;
				for (int j = length - 1; j >= 0; j--) {
					if (target[j] != buffer[k--] || k < start) {
						return -1;
					}
				}
				return i - length;
			}
		}
		return -1;
	}
}
