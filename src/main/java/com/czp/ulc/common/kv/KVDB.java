package com.czp.ulc.common.kv;

import java.nio.charset.Charset;
import java.util.List;

import com.alibaba.fastjson.JSONObject;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月28日 上午11:14:01</li>
 * 
 * @version 0.0.1
 */

public interface KVDB {

	Charset UTF8 = Charset.forName("utf-8");

	boolean append(String key, String values);

	boolean put(String key, String values);

	boolean put(String key, long values);

	String get(String key);

	String get(Integer key);

	String hget(String key, String haskKey);

	void hincr(String key, String haskKey);

	JSONObject hgetAll(String key);

	boolean hput(String key, String hashKey, Object obj);

	List<String> getList(String key);

	void put(byte[] key, byte[] value);

	void put(byte[] key, String value);

	String get(byte[] key);
	
	byte[] getBytes(byte[] key);

	void putInt(Integer key, String value);

	long getLong(String key, long dftVal);

}