package com.czp.ulc.web;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.czp.ulc.web.cmd.CmdHandlerManager;
import com.czp.ulc.web.cmd.ICommandHandler;

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
	private CmdHandlerManager manager;

	public ProcessorController() {

	}

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
	public String del(ProcessorBean arg) {
		Assert.notNull(arg.getId(), "id is required");
		ProcessorBean inDb = dao.selectOne(arg);
		if (inDb == null) {
			throw new RuntimeException("no such bean");
		}
		if (dao.delete(inDb) < 1) {
			throw new RuntimeException("del bean fail");
		}
		return "success";
	}

	@RequestMapping("/list")
	public List<ProcessorBean> list(ProcessorBean arg) {
		return dao.list(arg);
	}

	@RequestMapping("/getProcHosts")
	public List<HostBean> getProcHosts(String procName) {
		List<HostBean> hosts = dao.queryProcHost(procName);
		if (hosts.isEmpty()) {
			hosts = hDao.listSpec("name,id", null);
		}
		return hosts;
	}

	@RequestMapping("/listByName")
	public List<ProcessorBean> listByName(ProcessorBean arg) {
		return dao.query(arg);
	}

	@RequestMapping("/mgr")
	public List<String> manage(int id, @RequestParam String type) {
		ArrayList<String> res = new ArrayList<String>(1);
		ICommandHandler handler = manager.getHandler(type);
		if (handler == null) {
			res.add("unsupport command:" + type);
			return res;
		}

		ProcessorBean proc = dao.get(id);
		if (proc == null) {
			res.add("processor not found:" + id);
			return res;
		}
		HostBean host = hDao.get(proc.getHostId());
		if (host == null) {
			res.add("host not found:" + proc.getHostId());
			return res;
		}

		String result = handler.handler(type, proc, host);
		res.add(result);
		return res;
	}
}
