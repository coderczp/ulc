/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.rule;

import com.czp.ulc.common.bean.HostBean;

/**
 * Function:告警对象
 *
 * @date:2017年3月28日/下午5:53:21
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class AlarmBean {

	private Type type;
	private String file;
	private String lines;
	private HostBean host;

	public AlarmBean(HostBean host, String file, String lines, Type type) {
		this.host = host;
		this.file = file;
		this.lines = lines;
		this.type = type;
	}

	public static enum Type {
		EMAIL, SMS
	}

	public HostBean getHost() {
		return host;
	}

	public String getFile() {
		return file;
	}

	public String getLines() {
		return lines;
	}

	public Type getType() {
		return type;
	}

}
