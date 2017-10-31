package com.czp.ulc.module.lucene.search;

import java.io.Serializable;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月30日</li>
 * 
 * @version 0.0.1
 */

public class SearchResult implements Serializable {

	private static final long serialVersionUID = 1L;

	private boolean isFinish;

	private String host = "";

	private String line = "";

	private String file = "";

	private long matchCount;

	public long getMatchCount() {
		return matchCount;
	}

	public void setMatchCount(long matchCount) {
		this.matchCount = matchCount;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getLine() {
		return line;
	}

	public void setLine(String line) {
		this.line = line;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public boolean isFinish() {
		return isFinish;
	}

	public void setFinish(boolean isFinish) {
		this.isFinish = isFinish;
	}

}
