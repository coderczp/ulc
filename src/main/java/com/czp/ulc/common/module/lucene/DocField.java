package com.czp.ulc.common.module.lucene;

/**
 * 请添加描述 <li>创建人：Jeff.cao</li> <li>创建时间：2017年5月13日 下午3:28:59</li>
 * 
 * @version 0.0.1
 */

public interface DocField {
	String TIME = "t";
	String FILE = "f";
	String LINE = "l";
	String HOST = "h";
	String[] ALL_FEILD = { TIME, FILE, LINE, HOST };
	String[] NO_LINE_FEILD = { TIME, FILE,  HOST };
}