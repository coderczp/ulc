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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.collect.handler.LuceneLogHandler;
import com.czp.ulc.collect.handler.NumSupportQueryParser;
import com.czp.ulc.collect.handler.Searcher;
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
	private LuceneLogHandler luceneSearch;

	private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);

	@RequestMapping("/count")
	public JSONObject search(@RequestParam String file, String host, Long start, Long end) throws Exception {
		long now = System.currentTimeMillis();
		Set<String> hostSet = buildHost(host);
		long timeEnd = (end == null) ? now : end;
		long timeStart = (start == null) ? now - MILLS : start;
		return luceneSearch.count(file, hostSet, timeEnd, timeStart);
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

		if (!q.startsWith("line:")) {
			q = String.format("line:%s", q);
		}
		if (Utils.notEmpty(proc)) {
			q = String.format("%s AND file:%s", q, proc);
		}
		if (Utils.notEmpty(file)) {
			q = String.format("%s AND file:%s", q, file);
		}
		q = String.format("%s AND time:[%s TO %s]", q, timeStart, timeEnd);

		Set<String> queryFields = new HashSet<>();
		queryFields.add("file");

		if (loadLine != null && loadLine) {
			queryFields.add("line");
		}

		String[] fields = { "line", "time", "file" };
		NumSupportQueryParser parser = new NumSupportQueryParser(fields, luceneSearch.getAnalyzer());
		parser.addSpecFied("time", LongPoint.class);

		AtomicLong matchCount = new AtomicLong();
		JSONObject data = new JSONObject();
		Searcher search = new Searcher() {

			@Override
			@SuppressWarnings({ "unchecked" })
			public boolean handle(String host, Document doc, long total) {

				matchCount.set(total);
				String file = doc.get("file");
				String line = doc.get("line");

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
				if (line != null) {
					lines.add(line);
				}
				return true;
			}

		};

		search.setQuery(parser.parse(q));
		search.setFields(queryFields);
		search.setBegin(timeStart);
		search.setHosts(hosts);
		search.setEnd(timeEnd);
		search.setSize(size);

		long allDocs = luceneSearch.search(search);
		long cost = System.currentTimeMillis() - now;

		JSONObject res = new JSONObject();
		res.put("data", data);
		res.put("count", allDocs);
		res.put("time", cost / 1000.0);
		res.put("matchCount", matchCount.get());
		LOG.info("query:[{}] time:{}ms", q, cost);
		return res;
	}

	private Set<String> buildHost(String host) {
		if (!Utils.notEmpty(host))
			return null;
		Set<String> hostSet = new HashSet<>();
		for (String string : host.split(",")) {
			hostSet.add(string);
		}
		return hostSet;
	}
}
