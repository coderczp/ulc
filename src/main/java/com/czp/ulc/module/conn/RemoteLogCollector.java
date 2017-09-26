/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.module.conn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.core.bean.HostBean;
import com.czp.ulc.core.bean.MonitorConfig;
import com.czp.ulc.core.dao.MonitoConfigDao;
import com.czp.ulc.core.message.Message;
import com.czp.ulc.core.message.MessageCenter;
import com.czp.ulc.core.message.MessageListener;
import com.czp.ulc.util.Utils;
import com.jcraft.jsch.ChannelShell;

/**
 * Function:远程log收集器
 *
 * @date:2017年3月21日/下午7:41:47
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class RemoteLogCollector implements Runnable, MessageListener<MonitorConfig> {

	protected HostBean server;
	protected MonitoConfigDao mfdao;
	protected int retryTimes = 0;
	protected ChannelShell shell;
	protected int maxRetryTimes = 50;
	protected volatile boolean isRunning = true;

	protected ConnectManager connManager;
	protected MessageCenter messageCenter;
	private static Logger LOG = LoggerFactory.getLogger(RemoteLogCollector.class);
	private static ConcurrentHashMap<Integer, Boolean> monitorHosts = new ConcurrentHashMap<>();

	public RemoteLogCollector(HostBean server, MonitoConfigDao dao, ConnectManager connMgr, MessageCenter mqCenter) {
		this.mfdao = dao;
		this.server = server;
		this.connManager = connMgr;
		this.messageCenter = mqCenter;
		messageCenter.addConcumer(this);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run() {
		try {

			JSONObject cmd = locadConfig(server);
			if (cmd == null) {
				LOG.info("host {} hasn't add monitor file,so this thread will exit", server);
				return;
			}

			String tailCmd = cmd.getString("tail");
			Set<String> excludeFiles = (Set<String>) cmd.get("exclude");
			HashMap<String, Boolean> map = new HashMap<String, Boolean>();

			shell = (ChannelShell) connManager.openChannel(server, "shell");
			if (shell == null) {
				LOG.info("{} may be deleted,so this thread will exit", server);
				return;
			}
			retryTimes = 0;
			LOG.info("monitor:{} cmd:{}", server, tailCmd);

			PipedOutputStream src = new PipedOutputStream();
			PipedOutputStream srcSend = new PipedOutputStream();
			PipedInputStream readResp = new PipedInputStream(src);
			PipedInputStream pisSend = new PipedInputStream(srcSend);
			shell.setInputStream(pisSend);
			shell.setOutputStream(src);
			shell.connect();

			sendCommad(srcSend, tailCmd);

			int lineLen = 0;
			String line = null;
			String nowFile = "";
			int prefuxSize = "==>".length();
			BufferedReader bufRead = new BufferedReader(new InputStreamReader(readResp));
			while (isRunning && (line = bufRead.readLine()) != null) {
				line = line.trim();
				lineLen = line.length();
				if (lineLen == 0) {
					LOG.debug("read empty line:{}", line);
					continue;
				}

				if (line.startsWith("==>")) {
					nowFile = line.substring(prefuxSize, lineLen - prefuxSize).trim();
					LOG.debug("read file name line:{}", line);
					continue;
				}

				if (isExcludeFile(nowFile, excludeFiles, map)) {
					LOG.debug("skip:{}{}{}", server.getName(), nowFile, line);
					continue;
				}
				messageCenter.push(new Message(new ReadResult(server, nowFile, line)));
			}
			monitorHosts.remove(server.getId());
			bufRead.close();
			doRetryConnect();
		} catch (Exception e) {
			LOG.error("read log error", e);
			doRetryConnect();
		}
	}

	/***
	 * 是否跳过当前文件
	 * 
	 * @param file
	 * @param excludeFiles
	 * @param map
	 * 
	 * @return
	 */
	private boolean isExcludeFile(String file, Set<String> excludeFiles, HashMap<String, Boolean> map) {
		if (file == null || file.length() == 0)
			return false;
		Boolean boolean1 = map.get(file);
		if (boolean1 != null && boolean1)
			return true;
		// 这方法调用非常频繁,用map加速
		for (String tmpFile : excludeFiles) {
			if (file.endsWith(tmpFile)) {
				map.put(file, Boolean.TRUE);
				return true;
			}
			map.put(file, Boolean.FALSE);
		}
		return false;
	}

	/***
	 * 加载主机的配置信息
	 * 
	 * @param server
	 * @return
	 */
	private JSONObject locadConfig(HostBean server) {
		List<MonitorConfig> list = queryHostMonitorFile(server);
		if (list.isEmpty()) {
			list = queryGlobMonitorFile();
		}

		if (list.isEmpty())
			return null;

		StringBuilder tail = new StringBuilder("tail -n 1 -F ");
		Set<String> exclude = new HashSet<String>();
		for (int i = 0; i < list.size(); i++) {
			MonitorConfig obj = list.get(i);
			String file = obj.getFile();
			if (tail.indexOf(file) > -1)
				continue;

			tail.append(obj.getFile()).append(" ");
			String excludeFile = obj.getExcludeFile();
			if (!Utils.notEmpty(excludeFile))
				continue;

			for (String str : excludeFile.split(",")) {
				exclude.add(str);
			}
		}
		JSONObject json = new JSONObject();
		json.put("tail", tail.toString());
		json.put("exclude", exclude);
		return json;
	}

	/***
	 * 查询全部监控配置
	 * 
	 * @return
	 */
	private List<MonitorConfig> queryGlobMonitorFile() {
		MonitorConfig arg = new MonitorConfig();
		arg.setHostId(-1);
		return mfdao.list(arg);
	}

	/***
	 * 查询当前主机的监控配置
	 * 
	 * @param server
	 * @return
	 */
	private List<MonitorConfig> queryHostMonitorFile(HostBean server) {
		MonitorConfig arg = new MonitorConfig();
		arg.setHostId(server.getId());
		return mfdao.list(arg);
	}

	// // tail: /xxx/xxx/gc.log: file truncated
	// // tail: `/var/logs/error.log' has appeared; following end of new file
	// private String isNewFile(String line, String lastFile) {
	// int splitSize = " ==>".length();
	// if (line.startsWith("==>")
	// || (line.startsWith("tail:") && (line.endsWith("file truncated") || line
	// .endsWith("following end of new file")))) {
	// lastFile = line.substring(splitSize, line.length() -
	// splitSize).trim().toLowerCase();
	// }
	// return lastFile;
	// }

	/***
	 * 网络断开后自动重连
	 */
	protected void doRetryConnect() {
		if (connManager.isNotReConn(server.getName()))
			return;
		if (connManager.isShutdown())
			return;

		if (retryTimes++ < maxRetryTimes) {
			LOG.info("retry connect:{}, times:{}", server, retryTimes);
			Utils.sleep(3000);
			run();
		}
	}

	/***
	 * 执行命令
	 * 
	 * @param out
	 * @param cmd
	 * @throws IOException
	 */
	protected void sendCommad(OutputStream out, String cmd) throws IOException {
		out.write(cmd.getBytes());
		out.write('\n');
		out.flush();
	}

	@Override
	public Class<MonitorConfig> processClass() {
		return MonitorConfig.class;
	}

	@Override
	public boolean onMessage(MonitorConfig message, Map<String, Object> ext) {
		String type = String.valueOf(ext.get("type"));
		if (message.getHostId().equals(server.getId()) && "update".equals(type)) {
			// 修改了监控文件,则触发重连
			this.shell.disconnect();
		}
		return false;
	}

	@Override
	public void onExit() {
		isRunning = false;
		connManager.disconnect(server);
	}
}
