package com.czp.ulc.web;

import java.util.ArrayList;
import java.util.Iterator;
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

import com.alibaba.fastjson.JSONObject;
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
		return ConnectManager.getInstance().exe(host, getCmd());
	}

	@RequestMapping("/restart")
	public List<String> restart(String host, String path) {
		String cmd = String.format("cd %s;./service.sh restart", path);
		return ConnectManager.getInstance().exe(host, cmd);
	}

	@RequestMapping("/search")
	public List<JSONObject> search(@RequestParam String proc) {
		String cmd = getCmd().concat("|grep ").concat(proc);
		List<JSONObject> res = new ArrayList<JSONObject>();
		Map<String, List<String>> result = ConnectManager.getInstance().exeInAll(cmd);
		for (Entry<String, List<String>> entry : result.entrySet()) {
			for (String it : entry.getValue()) {
				if (it.contains(proc)) {
					JSONObject json = new JSONObject();
					json.put("host", entry.getKey());
					json.put("proc", it);
					res.add(json);
				}
			}
		}
		return res;
	}

	private String getCmd() {
		String cmd = env.getProperty("process_list_cmd");
		Assert.notNull(cmd, "process_list_cmd is nul,please add in properties");
		return cmd;
	}
}
