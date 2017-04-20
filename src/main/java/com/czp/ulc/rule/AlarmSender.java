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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.common.MessageListener;

/**
 * Function:告警发送中心
 *
 * @date:2017年3月28日/下午5:50:50
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class AlarmSender implements MessageListener<AlarmBean> {

	private static final AlarmSender INSTANCE = new AlarmSender();
	private static Logger log = LoggerFactory.getLogger(AlarmSender.class);

	private AlarmSender() {
	}

	public static AlarmSender getInstance() {
		return INSTANCE;
	}

	@Override
	public void onExit() {
	}

	@Override
	public Class<AlarmBean> processClass() {
		return AlarmBean.class;
	}

	@Override
	public boolean onMessage(AlarmBean message, Map<String, Object> ext) {
		log.info("{}#{}", message.getHost().getName(), message.getFile());
		log.error(message.getLines());
		return false;
	}

}
