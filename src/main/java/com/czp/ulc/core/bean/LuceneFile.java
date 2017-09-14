package com.czp.ulc.core.bean;

import java.io.Serializable;

import javax.persistence.Transient;

/**
 * @dec Function
 * @author coder_czp@126.com
 * @date 2017年9月9日/上午10:12:25
 * @copyright coder_czp@126.com
 *
 */
public class LuceneFile implements Serializable {

	private static final long serialVersionUID = 1L;

	private int id;

	/** 当前纪录在哪个机器 */
	private String host;

	/** 当前纪录是哪个被监控机器的 */
	private String server;

	/** 当前纪录的时间 */
	private long itime;

	/** 纪录的完整路径 */
	private String path;

	/** 仅查询用 */
	@Transient
	private long start;

	/** 仅查询用 */
	@Transient
	private long end;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public long getItime() {
		return itime;
	}

	public void setItime(long itime) {
		this.itime = itime;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

}
