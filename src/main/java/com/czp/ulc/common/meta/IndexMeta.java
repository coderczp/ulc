package com.czp.ulc.common.meta;

/**
 * 请添加描述 <li>创建人：Jeff.cao</li> <li>创建时间：2017年5月13日 下午5:18:51</li>
 * 
 * @version 0.0.1
 */

public class IndexMeta {

	private long lines;
	private long docs;
	private long bytes;
	public static final IndexMeta EMPTY = new IndexMeta();

	public IndexMeta() {
	}

	public void setLines(long lines) {
		this.lines = lines;
	}

	public void setDocs(long docs) {
		this.docs = docs;
	}

	public void setBytes(long bytes) {
		this.bytes = bytes;
	}

	public long getLines() {
		return lines;
	}

	public long getDocs() {
		return docs;
	}

	public long getBytes() {
		return bytes;
	}

	public void updateRAMLines(int i) {
		lines += i;
	}

}
