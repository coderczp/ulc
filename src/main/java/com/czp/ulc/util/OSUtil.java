package com.czp.ulc.util;

import com.alibaba.fastjson.JSONObject;

/**
 * JVM信息收集类
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月14日 下午4:39:55</li>
 * 
 * @version 0.0.1
 */

public class OSUtil {

	private static long MB_SIZE = 1024 * 1024;

	/***
	 * 收集当前虚拟机的信息
	 * 
	 * @return
	 */
	public static JSONObject collectVMInfo() {
		JSONObject json = new JSONObject();
		Runtime rt = Runtime.getRuntime();
		int cpus = rt.availableProcessors();
		int nThread = Thread.activeCount();
		long totalMem = rt.totalMemory();
		long freeMem = rt.freeMemory();
		long maxMem = rt.maxMemory();

		json.put("memTotal", totalMem / MB_SIZE);
		json.put("freeMem", freeMem / MB_SIZE);
		json.put("maxMem", maxMem / MB_SIZE);
		json.put("thread", nThread);
		json.put("cpus", cpus);

		return json;
	}
	
	
}
