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

import com.czp.ulc.core.ArgException;
import com.czp.ulc.core.bean.MonitorConfig;
import com.czp.ulc.core.dao.MonitoConfigDao;
import com.czp.ulc.core.message.Message;
import com.czp.ulc.core.message.MessageCenter;

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
	private MessageCenter messageCenter;

	@RequestMapping("/add")
	public MonitorConfig addConfigFile(@Valid MonitorConfig bean, BindingResult result) {
		if (result.hasErrors()) {
			throw new ArgException(result);
		}
		if (dao.insertUseGeneratedKeys(bean) > 0) {
			notifyUpdate(bean, "update");
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
