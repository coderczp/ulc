package com.czp.ulc.web.cmd;

import org.springframework.stereotype.Component;

import com.czp.ulc.core.bean.ProcessorBean;

/**
 * 执行部署命令
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月10日</li>
 * 
 * @version 0.0.1
 */
@Component
public class DeployCmdHandler extends AbstractCmdHandler {

	@Override
	public String cmd() {
		return "deploy";
	}

	@Override
	protected String buildShellCmd(String type, ProcessorBean proc) {
		String path = proc.getPath();
		return String.format("cd %s;./service.sh all", path);
	}

}
