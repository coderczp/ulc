package com.czp.ulc.web.cmd;

import org.springframework.stereotype.Component;

import com.czp.ulc.core.bean.ProcessorBean;

/**
 * 压缩tomcat7/logs/history下的log
 * 
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月10日</li>
 * 
 * @version 0.0.1
 */
@Component
public class CompressLogCmdHandler extends AbstractCmdHandler {

	private static final String LOG_PATH = "tomcat7/logs/history";

	@Override
	public String cmd() {
		return "tarlog";
	}

	@Override
	protected String buildShellCmd(String type, ProcessorBean proc) {
		String path = proc.getPath();
		return String.format("cd %s;tar -czvf history.tar %s/* --remove-files &", path, LOG_PATH);
	}

}
