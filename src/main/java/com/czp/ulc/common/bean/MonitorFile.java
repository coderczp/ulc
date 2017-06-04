/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.common.bean;

/**
 * Function:监控的文件
 *
 * @date:2017年6月1日/上午9:59:06
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class MonitorFile {

	/** 主键ID */
	private int id;

	/** 分片ID */
	private int shard;

	/** 文件名 */
	private String file;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getShard() {
		return shard;
	}

	public void setShard(int shard) {
		this.shard = shard;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

}
