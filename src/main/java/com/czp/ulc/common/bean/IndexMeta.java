package com.czp.ulc.common.bean;

/**
 * 请添加描述 <li>创建人：Jeff.cao</li> <li>创建时间：2017年5月13日 下午5:18:51</li>
 * 
 * @version 0.0.1
 */

public class IndexMeta {

	private int id;
	/** 当前的数据行数 **/
	private long lines;
	/** 当前文档数 */
	private long docs;
	/** 当前收集的数据字节数 **/
	private long bytes;
	/** 每个分片收集数据的最晚时间 **/
	private long time;
	/** 分片ID */
	private int shardId;

	public IndexMeta() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
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

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public int getShardId() {
		return shardId;
	}

	public void setShardId(int shardId) {
		this.shardId = shardId;
	}

}
