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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.core.ArgException;
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
			throw new ArgException(result);
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
	public List<HostBean> list(HostBean bean) {
		List<HostBean> list = dao.listSpec("id,name,host", bean);
		return list;
	}
	
	@RequestMapping("/listSpec")
	public Object listSpec(HostBean bean) {
		JSONArray items = new JSONArray();
		ConnectManager mgr = getConnMgr();
		List<HostBean> list = dao.listSpec("name,host,status", bean);
		String cmd = "cat /proc/cpuinfo| grep 'processor'| wc -l;cat /etc/issue; cat /proc/meminfo|head -n2;df -h";
		for (HostBean hostBean : list) {
			List<String> res = mgr.exe(hostBean.getName(), cmd);
			JSONObject host = (JSONObject) JSONObject.toJSON(hostBean);
			if (res.size()>4) {
				/*****
				 * <pre>
				 *  
				8
				CentOS release 6.8 (Final)
				Kernel \r on an \m
				
				MemTotal:       32747444 kB
				MemFree:          228056 kB
				Filesystem            Size  Used Avail Use% Mounted on
				/dev/mapper/          3.6T  3.2T  275G  93% /
				tmpfs                  16G     0   16G   0% /dev/shm
				/dev/sda2             477M   48M  404M  11% /boot
				/dev/sda1             200M  264K  200M   1% /boot/efi
				 * <pre>
				 */
				String os = res.get(1);
				String cpus = res.get(0);
				host.put("os", os);
				host.put("cpus", cpus);
				try {
					String memFree = res.get(5).split(" +")[1];
					String memTotal = res.get(4).split(" +")[1];
					host.put("mem_free", Integer.valueOf(memFree)/1024);
					host.put("mem_total", Integer.valueOf(memTotal)/1024);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
			items.add(host);
		}
		return items;
	}

	private void notifyConnectChange(HostBean bean, String type) {
		JSONObject ext = new JSONObject();
		ext.put("type", type);
		messageCenter.push(new Message(bean, ext));
	}

}
