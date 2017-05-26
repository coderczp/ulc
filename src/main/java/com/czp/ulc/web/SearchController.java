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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.collect.handler.LogIndexHandler;
import com.czp.ulc.collect.handler.SearchCallback;
import com.czp.ulc.common.lucene.DocField;
import com.czp.ulc.common.lucene.RangeQueryParser;
import com.czp.ulc.common.util.Utils;

/**
 * Function:搜索接口
 *
 * @date:2017年3月24日/下午3:19:37
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
@RestController
public class SearchController {

	private static final long MILLS = 60 * 60 * 1000;

	@Autowired
	private LogIndexHandler luceneSearch;
	private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);

	@RequestMapping("/count")
	public JSONObject search(@RequestParam String file, String host, String proc, Long start, Long end)
			throws Exception {
		long now = System.currentTimeMillis();
		Set<String> hosts = buildHost(host);
		long timeEnd = (end == null) ? now : end;
		long timeStart = (start == null) ? now - MILLS : start;
		String q = String.format("%s:%s", DocField.FILE, file);
		if (Utils.notEmpty(proc)) {
			q = String.format("%s AND %s:%s", q, DocField.FILE, proc);
		}

		String[] fields = new String[] { DocField.FILE };
		RangeQueryParser parser = new RangeQueryParser(fields, luceneSearch.getAnalyzer());
		parser.addSpecFied(DocField.TIME, LongPoint.class);
		SearchCallback search = new SearchCallback();
		search.setQuery(parser.parse(q));
		search.setBegin(timeStart);
		search.setHosts(hosts);
		search.setEnd(timeEnd);
		JSONObject count = luceneSearch.count(search);
		JSONObject res = new JSONObject();

		res.put("data", count);
		res.put("time", (System.currentTimeMillis() - now) / 1000);
		return count;
	}

	@RequestMapping("/meta")
	public JSONObject meta() throws Exception {
		return (JSONObject) JSONObject.toJSON(luceneSearch.getMeta());
	}

	@RequestMapping("/getFile")
	public void getFile(@RequestParam String json, HttpServletResponse out) throws Exception {
		long now = System.currentTimeMillis();
		JSONObject obj = JSONObject.parseObject(json);

		String q = obj.getString("q");
		int size = obj.getIntValue("size");
		String file = obj.getString("file");
		Set<String> hosts = buildHost(obj.getString("host"));
		long timeEnd = obj.containsKey("end") ? obj.getLongValue("end") : now;
		long timeStart = obj.containsKey("start") ? obj.getLongValue("start") : now;

		q = String.format("%s:%s", DocField.LINE, q);
		q = String.format("%s AND %s:%s", q, DocField.FILE, file);
		q = escape(q, DocField.ALL_FEILD);
		q = String.format("%s AND %s:[%s TO %s]", q, DocField.TIME, timeStart, timeEnd);

		RangeQueryParser parser = new RangeQueryParser(DocField.ALL_FEILD, luceneSearch.getAnalyzer());
		parser.addSpecFied(DocField.TIME, LongPoint.class);

		out.setContentType("text/plain");
		PrintWriter writer = out.getWriter();
		SearchCallback search = new SearchCallback() {

			@Override
			public boolean handle(String host, String file, String line, long matchs, long allLines) {
				writer.println(line);
				return true;
			}
		};

		search.addFeild(DocField.FILE);
		search.addFeild(DocField.TIME);
		search.addFeild(DocField.HOST);
		search.addFeild(DocField.LINE);
		search.setQuery(parser.parse(q));

		search.setBegin(timeStart);
		search.setHosts(hosts);
		search.setEnd(timeEnd);
		search.setSize(Math.min(1000, size));

		luceneSearch.search(search);
		out.getWriter().close();
	}

	@RequestMapping("/search")
	public JSONObject search(@RequestParam String json) throws Exception {
		long now = System.currentTimeMillis();
		JSONObject obj = JSONObject.parseObject(json);

		String q = obj.getString("q");
		int size = obj.getIntValue("size");
		String proc = obj.getString("proc");
		String file = obj.getString("file");
		Boolean loadLine = obj.getBoolean("loadLine");
		Set<String> hosts = buildHost(obj.getString("host"));
		long timeEnd = obj.containsKey("end") ? obj.getLongValue("end") : now;
		long timeStart = obj.containsKey("start") ? obj.getLongValue("start") : now;

		if (!q.startsWith(DocField.LINE)) {
			q = String.format("%s:%s", DocField.LINE, q);
		}
		if (Utils.notEmpty(proc)) {
			q = String.format("%s AND %s:%s", q, DocField.FILE, proc);
		}
		if (Utils.notEmpty(file)) {
			q = String.format("%s AND %s:%s", q, DocField.FILE, file);
		}
		q = escape(q, DocField.ALL_FEILD);
		q = String.format("%s AND %s:[%s TO %s]", q, DocField.TIME, timeStart, timeEnd);

		RangeQueryParser parser = new RangeQueryParser(DocField.ALL_FEILD, luceneSearch.getAnalyzer());
		parser.addSpecFied(DocField.TIME, LongPoint.class);

		AtomicLong allLine = new AtomicLong();
		AtomicLong matchCount = new AtomicLong();
		JSONObject data = new JSONObject();
		SearchCallback search = new SearchCallback() {

			@Override
			@SuppressWarnings({ "unchecked" })
			public boolean handle(String host, String file, String line, long matchs, long allLines) {
				allLine.set(allLines);
				matchCount.set(matchs);

				JSONObject files = data.getJSONObject(host);
				if (files == null) {
					files = new JSONObject();
					data.put(host, files);
				}

				List<String> lines = (List<String>) files.get(file);
				if (lines == null) {
					lines = new LinkedList<>();
					files.put(file, lines);
				}
				if (line != null)
					lines.add(line);

				return true;
			}
		};

		search.addFeild(DocField.FILE);
		search.addFeild(DocField.TIME);
		search.addFeild(DocField.HOST);

		if (Boolean.TRUE.equals(loadLine))
			search.addFeild(DocField.LINE);

		search.setQuery(parser.parse(q));
		search.setBegin(timeStart);
		search.setHosts(hosts);
		search.setEnd(timeEnd);
		search.setSize(size);

		long allDocs = luceneSearch.search(search);
		double cost = (System.currentTimeMillis() - now) / 1000.0;

		JSONObject res = new JSONObject();
		res.put("data", data);
		res.put("time", cost);
		res.put("docCount", allDocs);
		res.put("lineCount", allLine.get());
		res.put("matchCount", matchCount.get());
		LOG.info("query:[{}] time:{}s", q, cost);
		return res;
	}

	private String escape(String q, String[] allFeild) {
		// 先把所有DocField.ALL_FEILD开通的替换如: 空白l:error->空白l#error
		for (String string : allFeild) {
			if (q.startsWith(string)) {
				q = q.replaceAll(String.format("%s:", string), String.format(" %s#", string));
			} else {
				q = q.replaceAll(String.format(" %s:", string), String.format(" %s#", string));
			}
		}
		// 将查询转义
		q = QueryParser.escape(q);
		// 恢复域字段
		for (String string : allFeild) {
			if (q.startsWith(string)) {
				q = q.replaceAll(String.format("%s#", string), String.format(" %s:", string));
			} else {
				q = q.replaceAll(String.format(" %s#", string), String.format(" %s:", string));
			}
		}
		return q;
	}

	private Set<String> buildHost(String host) {
		Set<String> hostSet = new HashSet<>();
		if (!Utils.notEmpty(host))
			return hostSet;
		for (String string : host.split(",")) {
			hostSet.add(string);
		}
		return hostSet;
	}
}
