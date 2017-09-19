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

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.core.ArgInvalideException;
import com.czp.ulc.core.bean.HostBean;
import com.czp.ulc.core.dao.HostDao;
import com.czp.ulc.core.message.Message;
import com.czp.ulc.core.message.MessageCenter;
import com.czp.ulc.module.conn.ConnectManager;
import com.czp.ulc.util.Utils;

/**
 * Function:主机信息管理接口
 *
 * @date:2017年3月28日/上午9:39:19
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
@RestController
@RequestMapping("/host")
public class HostController {

	@Autowired
	private HostDao dao;

	@Autowired
	private MessageCenter messageCenter;

	@Autowired
	private ApplicationContext context;

	@RequestMapping("/add")
	public HostBean addHost(@Valid HostBean bean, BindingResult result) throws Exception {
		if (result.hasErrors()) {
			throw new ArgInvalideException(result);
		}
		bean.setPwd(Utils.encrypt(bean.getPwd()));
		getConnMgr().connect(bean);
		dao.insertUseGeneratedKeys(bean);
		return bean;
	}

	private ConnectManager getConnMgr() {
		return context.getBean(ConnectManager.class);
	}

	@RequestMapping("/del")
	public HostBean delHost(HostBean bean) {
		if (dao.delete(bean) > 0) {
			notifyConnectChange(bean, "delete");
		}
		return bean;
	}

	@RequestMapping("/update")
	public HostBean update(HostBean bean) {
		if (dao.updateByPrimaryKey(bean) > 0) {
			notifyConnectChange(bean, "update");
		}
		return bean;
	}

	@RequestMapping("/list")
	public List<HostBean> list(String json) {
		List<HostBean> list = (json != null) ? dao.list(JSONObject.parseObject(json)) : dao.list(null);
		for (HostBean hostBean : list) {
			hostBean.setPwd(null);
		}
		return list;
	}

	private void notifyConnectChange(HostBean bean, String type) {
		JSONObject ext = new JSONObject();
		ext.put("type", type);
		messageCenter.push(new Message(bean, ext));
	}

}
