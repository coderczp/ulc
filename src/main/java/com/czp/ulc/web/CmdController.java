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
	public List<JSONObject> search(@RequestParam String proc) {
		String cmd = env.getProperty("process_list_cmd");
		Assert.notNull(cmd, "process_list_cmd is nul,please add in properties");
		String cmd2 = cmd.concat("|grep ").concat(proc);
		Map<String, List<String>> exeInAll = ConnectManager.getInstance().exeInAll(cmd2);
		Set<Entry<String, List<String>>> entrySet = exeInAll.entrySet();
		List<JSONObject> res = new ArrayList<JSONObject>();
		for (Entry<String, List<String>> entry : entrySet) {
			Iterator<String> it = entry.getValue().iterator();
			while (it.hasNext()) {
				String next = it.next();
				if (next.contains(proc)) {
					JSONObject json = new JSONObject();
					json.put("host", entry.getKey());
					json.put("proc", next);
					res.add(json);
				}
			}
		}
		return res;
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
