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

import java.io.File;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.core.bean.IndexMeta;
import com.czp.ulc.core.bean.LuceneFile;
import com.czp.ulc.core.dao.LuceneFileDao;
import com.czp.ulc.main.Application;
import com.czp.ulc.module.lucene.AnalyzerUtil;
import com.czp.ulc.module.lucene.DocField;
import com.czp.ulc.module.lucene.LogAnalyzer;
import com.czp.ulc.module.lucene.search.LocalIndexSearcher;
import com.czp.ulc.module.lucene.search.SearchCallback;
import com.czp.ulc.util.OSUtil;

/**
 * Function:搜索接口
 *
 * @date:2017年3月24日/下午3:19:37
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
@RestController
@RequestMapping("/q")
public class SearchController {

	@Autowired
	private LuceneFileDao lFileDao;

	@RequestMapping("/count")
	public JSONObject count(@RequestParam String json) throws Exception {
		long start = System.currentTimeMillis();
		QueryCondtion cdt = createCdtFromJson(json);
		SearchCallback search = new SearchCallback(cdt);
		Map<String, Long> res = getSearcher().count(search);
		long now = System.currentTimeMillis();
		res.put("time", (now - start));
		return (JSONObject) JSONObject.toJSON(res);
	}

	@RequestMapping("/token")
	public String token(@RequestParam String str) throws Exception {
		return AnalyzerUtil.displayToken(str, new LogAnalyzer());
	}

	@RequestMapping("/file")
	public String listFile() throws Exception {
		return "";
	}
	
	@RequestMapping("/meta")
	public JSONObject meta() throws Exception {
		LuceneFile lFile = lFileDao.queryEarliestFile(null);
		IndexMeta meta = getSearcher().getMeta();
		JSONObject json = OSUtil.collectVMInfo();
		JSONObject metaJson = (JSONObject) JSONObject.toJSON(meta);
		json.putAll(metaJson);
		if (lFile != null) {
			json.put("minFile", new File(lFile.getPath()).getName());
		}
		return json;
	}

	@RequestMapping("/getFile")
	public void getFile(@RequestParam String json, HttpServletRequest req, HttpServletResponse out) throws Exception {
		QueryCondtion cdt = createCdtFromJson(json);
		String file = cdt.getFile();
		cdt.setSize(Math.min(1000, cdt.getSize()));
		cdt.setFile(file.substring(file.lastIndexOf("/") + 1));
		out.setCharacterEncoding("utf-8");

		out.setContentType("text/plain;charset=utf-8");
		PrintWriter writer = out.getWriter();

		CountDownLatch lock = new CountDownLatch(1);
		writer.println(String.format("host:%s,file:%s,keyword:%s", cdt.getHosts(), cdt.getFile(), cdt.getQ()));

		getSearcher().search(new SearchCallback(cdt, DocField.ALL_FEILD) {

			@Override
			public boolean handle(String host, String file, String line) {
				writer.println(line);
				return true;
			}

			@Override
			public void onFinish(long allDoc, long allMatch) {
				writer.flush();
				writer.close();
				lock.countDown();
			}
		});
		lock.await(2, TimeUnit.MINUTES);
	}

	private QueryCondtion createCdtFromJson(String json) {
		QueryCondtion cdt = JSONObject.parseObject(json, QueryCondtion.class);
		cdt.setAnalyzer(getSearcher().getAnalyzer());
		return cdt;
	}

	@RequestMapping("/search")
	public Map<String, Object> search(@RequestParam String json, HttpServletRequest req, HttpServletResponse resp)
			throws Exception {

		CountDownLatch lock = new CountDownLatch(1);
		QueryCondtion cdt = createCdtFromJson(json);
		Map<String, Object> res = new ConcurrentHashMap<>();

		getSearcher().search(new SearchCallback(cdt, cdt.isLoadLine() ? DocField.ALL_FEILD : DocField.NO_LINE_FEILD) {

			JSONObject data = new JSONObject();
			AtomicLong hasAdd = new AtomicLong();
			long now = System.currentTimeMillis();

			@Override
			public boolean handle(String host, String file, String line) {
				hasAdd.getAndIncrement();
				JSONObject files = data.getJSONObject(host);
				if (files == null) {
					files = new JSONObject();
					data.put(host, files);
				}

				List<Object> lines = files.getJSONArray(file);
				if (lines == null) {
					lines = new LinkedList<>();
					files.put(file, lines);
				}
				if (line != null)
					lines.add(line);

				// return hasAdd.get() <= cdt.getSize();
				return hasAdd.get() < 1000;
			}

			@Override
			public void onFinish(long allDoc, long allMatch) {
				long end = System.currentTimeMillis();
				res.put("data", data);
				res.put("docCount", allDoc);
				res.put("lineCount", allDoc);
				res.put("time", (end - now));
				res.put("matchCount", allMatch);
				lock.countDown();
			}
		});
		lock.await(10, TimeUnit.MINUTES);
		return res;
	}

	public String escape(String q, String[] allFeild) {
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

	/** 不能用注入的方式,因为该类init是lucenmodule还没有加载 */
	private LocalIndexSearcher getSearcher() {
		return Application.getBean(LocalIndexSearcher.class);
	}
}
