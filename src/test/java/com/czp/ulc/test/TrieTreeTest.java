package com.czp.ulc.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.common.TrieTree;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月1日 上午11:13:06</li>
 * 
 * @version 0.0.1
 */

public class TrieTreeTest {

	private static String defaultUrl = "xxxx";

	// private String str =
	// "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	@Test
	public void testTrieTree() {
		int count = 20000;
		LinkedList<String> strs = new LinkedList<String>();
		for (int i = 0; i < count; i++) {
			strs.add(random(16));
		}
		TrieTree tree = new TrieTree();
		HashMap<String, Boolean> map = new HashMap<String, Boolean>();
		for (String string : strs) {
			map.put(string, true);
			tree.put(string);
		}
		long st = System.currentTimeMillis();
		for (String string : strs) {
			map.containsKey(string);
		}
		System.out.println((System.currentTimeMillis() - st));
		st = System.currentTimeMillis();
		for (String string : strs) {
			tree.contains(string);
		}
		System.out.println((System.currentTimeMillis() - st));
		System.out.println((System.currentTimeMillis() - st));

	}

	public static String random(int length) {
		StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			builder.append((char) (ThreadLocalRandom.current().nextInt(33, 128)));
		}
		return builder.toString();
	}

	public static void sendEmailHttp(JSONObject json) {
		try {

			URL url = new URL(defaultUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setDoInput(true);

			StringBuffer params = new StringBuffer();
			params.append("message=").append(json.toJSONString());
			conn.getOutputStream().write(params.toString().getBytes());
			conn.getOutputStream().flush();

			String line;
			BufferedReader bos = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer lines = new StringBuffer();
			while ((line = bos.readLine()) != null) {
				lines.append(line);
			}
			conn.disconnect();
			System.out.println(lines);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
