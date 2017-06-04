package com.czp.ulc.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.czp.ulc.collect.RemoteLogCollector;
import com.czp.ulc.common.ArgInvalideException;
import com.czp.ulc.common.Message;
import com.czp.ulc.common.MessageCenter;
import com.czp.ulc.common.bean.HostBean;
import com.czp.ulc.common.bean.MonitorConfig;
import com.czp.ulc.common.dao.HostDao;
import com.czp.ulc.common.dao.MonitoConfigDao;

/**
 * 监控文件管理接口
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年3月30日 下午4:15:08</li>
 * 
 * @version 0.0.1
 */

@RestController
@RequestMapping("/mfile")
public class MonitoFileController {

	@Autowired
	private MonitoConfigDao dao;

	@Autowired
	private HostDao hostDao;

	@Autowired
	private MessageCenter messageCenter;

	@RequestMapping("/add")
	public MonitorConfig addHost(@Valid MonitorConfig bean, BindingResult result) {
		if (result.hasErrors()) {
			throw new ArgInvalideException(result);
		}
		if (dao.insertUseGeneratedKeys(bean) > 0) {
			// 已经监控则重练则可
			if (RemoteLogCollector.hasMonitor(bean.getHostId())) {
				notifyUpdate(bean, "update");
			} else {
				// 没有则建立连接
				HostBean tmp = new HostBean();
				tmp.setId(bean.getHostId());
				HostBean host = hostDao.selectOne(tmp);
				RemoteLogCollector.monitorIfNotExist(host, dao);
			}
		}
		return bean;
	}

	@RequestMapping("/del")
	public MonitorConfig del(MonitorConfig arg) {
		Assert.notNull(arg.getId(), "id is required");
		MonitorConfig inDb = dao.selectOne(arg);
		if (inDb != null && dao.delete(arg) > 0) {
			notifyUpdate(inDb, "update");
		}
		return arg;
	}

	@RequestMapping("/list")
	public List<MonitorConfig> list(MonitorConfig arg) {
		return dao.list(arg);
	}

	private void notifyUpdate(MonitorConfig bean, String type) {
		Map<String, Object> ext = new HashMap<String, Object>();
		ext.put("type", type);
		Message message = new Message(bean, ext);
		messageCenter.push(message);
	}
}
