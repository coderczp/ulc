/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.czp.ulc.core.message.MessageCenter;
import com.czp.ulc.core.shutdown.ShutdownManager;
import com.czp.ulc.module.conn.ConnectionManager;
import com.czp.ulc.module.conn.RemoteLogCollector;

/**
 * Function:配置首页
 *
 * @date:2017年3月24日/下午4:30:24
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
@Controller
public class HomeController {

	@Autowired
	private MessageCenter mqServer;

	@Autowired
	private ApplicationContext context;

	@RequestMapping("/")
	public String index() {
		return "index.html";
	}

	@ResponseBody
	@RequestMapping("/pause")
	public String pause() {
		RemoteLogCollector.pause = !RemoteLogCollector.pause;
		return "change:" + RemoteLogCollector.pause;
	}

	@ResponseBody
	@RequestMapping("/stop")
	public String stop() {
		context.getBean(ConnectionManager.class).onExit();
		mqServer.stop();
		ShutdownManager.getInstance().run();
		SpringApplication.exit(context);
		return "app has stop";
	}
}
