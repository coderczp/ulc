package com.czp.ulc.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONArray;
import com.czp.ulc.collect.ConnectManager;

/**
 * @dec 执行命令
 * @author coder_czp@126.com
 * @date 2017年4月2日/下午12:17:44
 * @copyright coder_czp@126.com
 *
 */
@RestController
@RequestMapping("/proc")
public class CmdController {

	@Autowired
	private Environment env;

	@RequestMapping("/list")
	public List<String> exe(int hostId) {
		String cmd = env.getProperty("process_list_cmd");
		Assert.notNull(cmd, "process_list_cmd is nul,please add in properties");
		return ConnectManager.getInstance().exe(hostId, cmd);
	}

	@RequestMapping("/restart")
	public List<String> restart(Integer host, String path) {
		String cmd = String.format("cd %s;./service.sh restart", path);
		List<String> res = ConnectManager.getInstance().exe(host, cmd);
		return res;
	}

	/***
	 * 检查主机的进程状态
	 * 
	 * @param host
	 * @return
	 */
	@RequestMapping("/check")
	public JSONArray checkProcessStatus(int hostId) {
		List<String> procs = exe(hostId);
		
		return null;
	}

}
