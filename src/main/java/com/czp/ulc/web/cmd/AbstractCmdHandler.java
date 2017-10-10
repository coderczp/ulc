package com.czp.ulc.web.cmd;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.czp.ulc.core.bean.HostBean;
import com.czp.ulc.core.bean.ProcessorBean;
import com.czp.ulc.module.conn.ConnectManager;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月10日</li>
 * 
 * @version 0.0.1
 */
public abstract class AbstractCmdHandler implements ICommandHandler {

	@Autowired
	protected ApplicationContext context;

	protected static final Logger LOG = LoggerFactory.getLogger(AbstractCmdHandler.class);

	protected ConnectManager getConnMgr() {
		return context.getBean(ConnectManager.class);
	}

	@Override
	public String handler(String type, ProcessorBean proc, HostBean host) {
		String cmd = buildShellCmd(type, proc);
		LOG.info("start execute cmd:{}", cmd);
		List<String> exe = getConnMgr().exe(host.getName(), cmd);
		if (exe.isEmpty()) {
			exe.add("请检查是否有lock");
		}
		LOG.info("success execute cmd:{}", cmd);
		return exe.toString();
	}

	protected abstract String buildShellCmd(String type, ProcessorBean proc);

}
