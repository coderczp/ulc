package com.czp.ulc.test;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;

import com.czp.ulc.util.Utils;

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月14日 上午9:26:03</li>
 * 
 * @version 0.0.1
 */

public class TreemapTest {

	public static void main(String[] args) throws ParseException {
		TreeMap<Long, File> map = new TreeMap<>();
		SimpleDateFormat sp = new SimpleDateFormat("yyyy-MM-dd");
		for (File file : new File("./log/index").listFiles()) {
			Long time = sp.parse(file.getName()).getTime();
			map.put(time, file);
		}
		Long from = Utils.toDay(System.currentTimeMillis()).getTime();
		Long to = Utils.toDay(System.currentTimeMillis()).getTime();
		NavigableMap<Long, File> subMap = map.subMap(from, true, to, true);
		for (Entry<Long, File> string : subMap.entrySet()) {
            System.out.println(string.getKey().hashCode());
		}
		System.out.println("------------------------------");
		subMap = map.subMap(from, true, to, true);
		for (Entry<Long, File> string : subMap.entrySet()) {
			  System.out.println(string.getKey().hashCode());
		}
	}
}
