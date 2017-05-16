package com.czp.ulc.collect.handler;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.search.Query;

/**
 * 请添加描述 <li>创建人：Jeff.cao</li> <li>创建时间：2017年4月19日 上午8:58:08</li>
 * 
 * @version 0.0.1
 */

public class Searcher {

	private int size;

	private long begin;

	private long end;

	private Query query;

	private Set<String> hosts = new HashSet<>();

	public Set<String> getHosts() {
		return hosts;
	}

	public void setHosts(Set<String> hosts) {
		this.hosts = hosts;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public long getBegin() {
		return begin;
	}

	public void setBegin(long begin) {
		this.begin = begin;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	public Query getQuery() {
		return query;
	}

	public void setQuery(Query query) {
		this.query = query;
	}

	public boolean handle(String host, String file, String line, long matchCount, long allLines) {
		return false;
	}
}
