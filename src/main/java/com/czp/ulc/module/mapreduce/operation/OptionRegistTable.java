package com.czp.ulc.module.mapreduce.operation;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年11月1日</li>
 * 
 * @version 0.0.1
 */

public class OptionRegistTable {

	private ConcurrentHashMap<String, IOperation> tables = new ConcurrentHashMap<>();

	public void regist(IOperation op) {
		tables.put(op.type(), op);
	}

	public IOperation find(String type) {
		IOperation op = tables.get(type);
		return op;
	}
}
