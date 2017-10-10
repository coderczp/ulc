package com.czp.ulc.web.cmd;

import org.springframework.stereotype.Component;

import com.czp.ulc.core.bean.ProcessorBean;

/**
 * 强制重启
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月10日</li>
 * 
 * @version 0.0.1
 */
@Component
public class ForceRestartCmdHandler extends AbstractCmdHandler {

	@Override
	public String cmd() {
		return "frestart";
	}

	@Override
	protected String buildShellCmd(String type, ProcessorBean proc) {
		String path = proc.getPath();
		return String.format("cd %s;rm -rf lock;./service.sh restart", path);
	}

}
