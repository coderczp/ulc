package com.czp.ulc.web;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.czp.ulc.core.ArgInvalideException;
import com.czp.ulc.core.bean.HostBean;
import com.czp.ulc.core.bean.ProcessorBean;
import com.czp.ulc.core.dao.HostDao;
import com.czp.ulc.core.dao.ProcessorDao;
import com.czp.ulc.module.conn.ConnectManager;

/**
 * function
 *
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月18日-下午3:34:12</li>
 * 
 * @version 0.0.1
 */
@RestController
@RequestMapping("/proc")
public class ProcessorController {

	@Autowired
	private HostDao hDao;

	@Autowired
	private ProcessorDao dao;

	@Autowired
	private ApplicationContext context;

	private static final Logger LOG = LoggerFactory.getLogger(ProcessorController.class);

	@RequestMapping("/add")
	public ProcessorBean addConfigFile(@Valid ProcessorBean bean, BindingResult result) {
		if (result.hasErrors()) {
			throw new ArgInvalideException(result);
		}
		if (dao.insertUseGeneratedKeys(bean) < 1) {
			throw new RuntimeException("add bean fail");
		}
		return bean;
	}

	@RequestMapping("/del")
	public ProcessorBean del(ProcessorBean arg) {
		Assert.notNull(arg.getId(), "id is required");
		ProcessorBean inDb = dao.selectOne(arg);
		if (inDb == null && dao.delete(arg) > 0) {
			throw new RuntimeException("del bean fail");
		}
		return arg;
	}

	@RequestMapping("/list")
	public List<ProcessorBean> list(ProcessorBean arg) {
		return dao.query(arg);
	}

	@RequestMapping("/mgr")
	public List<String> manage(int id, @RequestParam String type) {
		ArrayList<String> res = new ArrayList<String>(1);
		ProcessorBean proc = dao.get(id);
		if (proc == null) {
			res.add("unsupport command:" + type);
		} else if (type.equals("stop") || type.equals("start") || type.equals("restart")) {
			HostBean hostBean = hDao.get(proc.getHostId());
			if (hostBean == null) {
				res.add("host not exist");
			} else {
				String path = proc.getPath().concat(proc.getName());
				String fmt = "cd %s;./service.sh %s";
				String host = hostBean.getName();
				String cmd = String.format(fmt, path, type);
				LOG.info("start execute cmd:{}", cmd);
				List<String> exe = getConnMgr().exe(host, cmd);
				LOG.info("success execute cmd");
				return exe;
			}
		} else {
			res.add("unsupport command:" + type);
		}
		return res;
	}

	private ConnectManager getConnMgr() {
		return context.getBean(ConnectManager.class);
	}
}
