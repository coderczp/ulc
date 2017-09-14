package com.czp.ulc.core.cache;

import java.nio.charset.Charset;

/**
 * 请添加描述 <li>创建人：Jeff.cao</li> <li>创建时间：2017年4月28日 上午11:14:01</li>
 * 
 * @version 0.0.1
 */

public interface KVDB {

	Charset UTF8 = Charset.forName("utf-8");

	void close();

	void delGroup(String group);

	int hincr(String gruop, byte[] key);

	byte[] hdel(String gruop, byte[] key);

	void hput(String gruop, byte[] key, byte[] value);

	void hget(String group, byte[] key, byte[] defaultVal);

}