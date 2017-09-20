package com.czp.ulc.web;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.core.ThreadPools;
import com.czp.ulc.core.bean.DeployRecord;
import com.czp.ulc.core.bean.HostBean;
import com.czp.ulc.core.bean.ProcessorBean;
import com.czp.ulc.core.dao.HostDao;
import com.czp.ulc.core.dao.IDeployRecordDao;
import com.czp.ulc.core.dao.ProcessorDao;
import com.czp.ulc.module.conn.ConnectManager;
import com.czp.ulc.util.MiniHeap;
import com.czp.ulc.util.Utils;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年8月9日 下午1:37:40</li>
 * 
 * @version 0.0.1
 */
@RestController
@RequestMapping("/project")
public class DeployController {

	@Autowired
	private HostDao dao;
	@Autowired
	private ProcessorDao pDao;
	@Autowired
	private IDeployRecordDao dDao;
	@Autowired
	private ApplicationContext context;

	private Map<String, Session> maps = new ConcurrentHashMap<>();

	private static final Logger LOG = LoggerFactory.getLogger(WebErrorHandler.class);

	private ExecutorService service = ThreadPools.getInstance().newPool("deploy-worker", 2);

	private JSch jSch = new JSch();

	@PreDestroy
	public synchronized void stop() {
		service.shutdown();
		maps.entrySet().forEach(it -> it.getValue().disconnect());
		maps.clear();
	}

	@RequestMapping("/check")
	public Object check(int id) {
		DeployRecord record = new DeployRecord();
		record.setId(id);
		record = dDao.selectOne(record);
		return record;
	}

	@RequestMapping("/queryLog")
	public Object queryLog(int id) {
		DeployRecord record = new DeployRecord();
		record.setId(id);
		String log = dDao.selectLog(id);
		return log;
	}

	@RequestMapping("/record")
	public Object record(int size) {
		return dDao.queryAll(size);
	}

	@RequestMapping("/listtar")
	public Object listtar(int size, String query) {
		FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				boolean b = name.endsWith(".tar") || name.endsWith(".gz");
				return query == null ? b : (b && name.contains(query));
			}
		};
		File[] listFiles = FileUploadConstroller.listFiles(filter);
		MiniHeap<File> maxFiles = new MiniHeap<>(size, new Comparator<File>() {

			@Override
			public int compare(File o1, File o2) {
				return (int) (o1.lastModified() - o2.lastModified());
			}
		});
		for (File file : listFiles) {
			maxFiles.add(file);
		}
		maxFiles.sort();
		List<String> files = new ArrayList<String>();
		for (int i = 0; i < maxFiles.size(); i++) {
			files.add(maxFiles.get(i).getName());
		}
		return files;
	}

	@RequestMapping("/getProc")
	public Object getProc(int hostId) {
		ProcessorBean example = new ProcessorBean();
		example.setHostId(hostId);
		return pDao.query(example);
	}

	@PostMapping("/deploy")
	public Object deploy(HttpServletRequest req, @RequestParam String tar, int hostId, int procId) {

		File tarFile = FileUploadConstroller.getPath(tar);
		if (!tarFile.exists()) {
			throw new RuntimeException(tar + " not exist,please upload frist");
		}
		HostBean host = dao.get(hostId);
		if (host == null) {
			throw new RuntimeException("host not exist,please check");
		}
		ProcessorBean proc = pDao.get(procId);
		if (proc == null) {
			throw new RuntimeException("project not exist,please check");
		}
		JSONObject json = new JSONObject();
		try {
			HttpSession session = req.getSession();
			String user = session.getAttribute("user").toString();

			String file = tarFile.getAbsolutePath();
			String projectName = proc.getName();
			String path = proc.getPath();
			if (!path.endsWith("/"))
				path = path.concat("/");

			String name = tarFile.getName();
			SimpleDateFormat spf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			String destPath = path.concat(name.substring(name.indexOf("_") + 1));

			DeployRecord record = new DeployRecord();
			record.setAuthor(user);
			record.setStatus("准备上传文件");
			record.setHost(host.getName());
			record.setProject(projectName);
			record.setTime(spf.format(new Date()));

			int res = dDao.insertUseGeneratedKeys(record);
			if (res > 0) {
				doDeploy(path, host, file, destPath, record.getId());
			} else {
				throw new RuntimeException("添加部署记录失败,请重试");
			}
			json.put("code", 200);
			json.put("id", record.getId());
		} catch (Exception e) {
			LOG.error("error", e);
			throw new RuntimeException(e.getMessage());
		}
		return json;
	}

	private void doDeploy(String path, HostBean host, String file, String destFile, Integer id) {
		service.execute(() -> {
			ChannelSftp ch = null;
			ChannelExec channel = null;
			ConnectManager connMgr = getConnMgr();
			try {
				updateStatus(id, "开始上传文件");
				Session session = createSession(host);
				updateStatus(id, "正在连接服务器");
				ch = (ChannelSftp) session.openChannel("sftp");
				ch.connect(1000 * 120);
				ch.put(file, destFile);

				updateStatus(id, "文件上传成功,准备重启");
				String cmd = String.format("cd %s;./service.sh all", path);
				LOG.info("upload file success,will exe:{}", cmd);
				channel = (ChannelExec) session.openChannel("exec");
				List<String> exe = connMgr.doExe(host.getName(), cmd, session);
				if (exe.isEmpty()) {
					updateStatus(id, "部署失败", "请检查工程目录是否有lock");
				} else {
					String log = exe.toString();
					if (log.length() > 6000) {
						log = log.substring(0, 6000);
					}
					updateStatus(id, "部署完成", log);
				}
			} catch (Throwable e) {
				LOG.error("error", e);
				StringWriter s = new StringWriter();
				e.printStackTrace(new PrintWriter(s));
				updateStatus(id, "部署失败,内部错误", s.toString());
			} finally {
				if (ch != null)
					ch.disconnect();
				if (channel != null)
					channel.disconnect();
			}
		});
	}

	public synchronized Session createSession(HostBean bean) throws JSchException {
		Session session = maps.get(bean.getName());
		if (session == null) {
			session = getConnMgr().getSession(bean);
		}
		if (session == null || !session.isConnected()) {
			session = jSch.getSession(bean.getUser(), bean.getHost(), bean.getPort());
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(Utils.decrypt(bean.getPwd()));
			session.connect(5000);
		}
		maps.put(bean.getName(), session);
		return session;
	}

	private ConnectManager getConnMgr() {
		return context.getBean(ConnectManager.class);
	}

	private DeployRecord updateStatus(Integer id, String status, String log) {
		DeployRecord record = new DeployRecord();
		record.setStatus(status);
		record.setLog(log);
		record.setId(id);
		dDao.updateByPrimaryKeySelective(record);
		return record;
	}

	private DeployRecord updateStatus(Integer id, String status) {
		return updateStatus(id, status, "");
	}
}
