/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.web;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Function:查询条件
 *
 * @date:2017年6月13日/上午9:30:10
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class QueryCondtion {

	/** 关键词 */
	private String q;

	/** 查询的文件 */
	private String file;

	/** 查询的进程 */
	private String proc;

	/** 返回的数据 */
	private int size;

	/** 是否加载内容行 */
	private boolean loadLine;

	/** 查询类型:search|cout|getFile */
	private String type;

	private long start = System.currentTimeMillis();

	/***
	 * 查询的域
	 */
	private Set<String> feilds = new HashSet<String>();

	/** 查询的主机 */
	private HashSet<String> hosts = new HashSet<String>();

	/** 结束时间 */
	private long end = start;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setHost(String host) {
		if (host != null && host.length() > 0)
			this.hosts = Sets.newHashSet(host.split(","));
	}

	public void setFeilds(Set<String> feilds) {
		this.feilds = feilds;
	}

	public Set<String> getFeilds() {
		return feilds;
	}

	public void addFeild(String... feild) {
		for (String item : feild) {
			feilds.add(item);
		}
	}

	public String getQ() {
		return q;
	}

	public void setQ(String q) {
		this.q = q;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getProc() {
		return proc;
	}

	public void setProc(String proc) {
		this.proc = proc;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public Set<String> getHosts() {
		return hosts;
	}

	public boolean isLoadLine() {
		return loadLine;
	}

	public void setLoadLine(boolean loadLine) {
		this.loadLine = loadLine;
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
