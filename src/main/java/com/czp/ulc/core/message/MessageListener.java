/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.core.message;

import java.util.EventListener;
import java.util.Map;

/**
 * Function:消息监听器
 *
 * @date:2017年3月21日/下午7:52:42
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public interface MessageListener<T> extends EventListener {

	void onExit();

	/**
	 * 处理的消息类
	 * 
	 * @return
	 */
	Class<T> processClass();

	/***
	 * 处理消息
	 * 
	 * @param message 
	 *             消息对象
	 * @param ext
	 *            扩展参数
	 * @return
	 */
	boolean onMessage(T message, Map<String, Object> ext);

}
