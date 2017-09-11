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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.Query;

import com.czp.ulc.common.module.lucene.DocField;
import com.czp.ulc.common.module.lucene.RangeQueryParser;
import com.czp.ulc.common.util.Utils;
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

	private Query query;

	private Analyzer analyzer;

	/** 开始时间 */
	private long start = System.currentTimeMillis();

	/** 查询的主机 */
	private Set<String> hosts = new HashSet<String>();

	/** 结束时间 */
	private long end = start;

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public void setAnalyzer(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	public Query getQuery() {
		return toQuery();
	}

	public void setHost(String host) {
		if(host!=null&&host.length()>0)
			this.hosts = Sets.newHashSet(host.split(","));
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

	private Query toQuery() {
		if (query != null)
			return query;

		try {
			RangeQueryParser parser = new RangeQueryParser(DocField.ALL_FEILD, analyzer);
			parser.addSpecFied(DocField.TIME, LongPoint.class);
			
			StringBuilder sb = new StringBuilder(String.format("%s:[%s TO %s]", DocField.TIME, start, end));

			if (Utils.notEmpty(proc)) {
				sb.append(String.format(" AND %s:%s", DocField.FILE, proc));
			}
			if (Utils.notEmpty(file)) {
				sb.append(String.format(" AND %s:%s", DocField.FILE, file));
			}
			if (Utils.notEmpty(q)) {
				sb.append(String.format(" AND %s:%s", DocField.LINE, q));
			}
			if (!hosts.isEmpty()) {
				sb.append(String.format(" AND %s:(", DocField.HOST));
				int size = hosts.size() - 1, i = 0;
				for (String string : hosts) {
					sb.append(string);
					if (i++ < size) {
						sb.append(" OR ");
					}
				}
				sb.append(")");
			}
			query =  parser.parse(sb.toString());
			return query;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
