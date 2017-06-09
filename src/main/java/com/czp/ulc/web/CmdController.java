package com.czp.ulc.web;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
	public List<String> exe(String host) {
		String cmd = env.getProperty("process_list_cmd");
		Assert.notNull(cmd, "process_list_cmd is nul,please add in properties");
		return ConnectManager.getInstance().exe(host, cmd);
	}

	@RequestMapping("/restart")
	public List<String> restart(String host, String path) {
		String cmd = String.format("cd %s;./service.sh restart", path);
		List<String> res = ConnectManager.getInstance().exe(host, cmd);
		return res;
	}

	@RequestMapping("/search")
	public Map<String, List<String>> search(@RequestParam String proc) {
		String cmd = env.getProperty("process_list_cmd");
		Assert.notNull(cmd, "process_list_cmd is nul,please add in properties");
		Map<String, List<String>> exeInAll = ConnectManager.getInstance().exeInAll(cmd);
		Set<Entry<String, List<String>>> entrySet = exeInAll.entrySet();
		for (Entry<String, List<String>> entry : entrySet) {
			List<String> value = entry.getValue();
			for (String string : value) {
				if (!string.contains(proc))
					value.remove(string);
			}
			if (value.isEmpty())
				exeInAll.remove(entry.getKey());
		}
		return exeInAll;
	}

	// /***
	// * 检查主机的进程状态
	// *
	// * @param host
	// * @return
	// */
	// @RequestMapping("/check")
	// public JSONArray checkProcessStatus(int hostId) {
	// List<String> procs = exe(hostId);
	//
	// return null;
	// }

}
