/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.common.mq;

import java.util.Map;

/**
 * Function:消息对象
 *
 * @date:2017年3月29日/上午10:58:48
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class Message {

	private Object message;
	private Map<String, Object> ext;

	public Message(Object message, Map<String, Object> ext) {
		this.message = message;
		this.ext = ext;
	}

	public Message(Object message) {
		this.message = message;
	}

	public Object getMessage() {
		return message;
	}

	public Map<String, Object> getExt() {
		return ext;
	}

}
