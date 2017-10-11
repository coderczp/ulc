package com.czp.ulc.web;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

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
import com.czp.ulc.module.conn.IExeCallBack;
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

	private ConcurrentHashMap<Integer, BlockingQueue<String>> logMap = new ConcurrentHashMap<>();

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
	public Object check(int procId) {
		try {
			while (!logMap.containsKey(procId)) {
				synchronized (logMap) {
					logMap.wait();
				}
			}

			BlockingQueue<String> queue = logMap.get(procId);
			String log = queue.take();
			StringBuilder sb = new StringBuilder(log);
			while (!queue.isEmpty()) {
				sb.append(queue.poll());
			}
			return sb;
		} catch (Exception e) {
			e.printStackTrace();
			return "time out";
		}
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
				boolean b = name.endsWith(".tar") || name.endsWith(".gz") || name.endsWith(".zip");
				return query == null ? b : (b && name.contains(query));
			}
		};
		File[] listFiles = FileUploadConstroller.listFiles(filter);
		List<String> files = sortByModifyTime(size, listFiles);
		return files;
	}

	private List<String> sortByModifyTime(int size, File[] listFiles) {
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

		if (logMap.containsKey(procId))
			throw new RuntimeException(tar + " 发布中,不允许并行发布");

		File tarFile = FileUploadConstroller.getPath(tar);
		if (!tarFile.exists()) {
			throw new RuntimeException(tar + " 找不到,请上传");
		}
		HostBean host = dao.get(hostId);
		if (host == null) {
			throw new RuntimeException("所选主机为配置");
		}
		ProcessorBean proc = pDao.get(procId);
		if (proc == null) {
			throw new RuntimeException("所选工程未配置");
		}
		JSONObject json = new JSONObject();
		try {
			HttpSession session = req.getSession();
			String user = session.getAttribute("user").toString();

			String projectName = proc.getName();
			String path = proc.getPath();
			if (!path.endsWith("/"))
				path = path.concat("/");

			SimpleDateFormat spf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

			DeployRecord record = new DeployRecord();
			record.setAuthor(user);
			record.setStatus("准备上传文件");
			record.setHost(host.getName());
			record.setProject(projectName);
			record.setTime(spf.format(new Date()));

			int res = dDao.insertUseGeneratedKeys(record);
			if (res > 0) {
				doDeploy(path, host, proc, tarFile, record.getId());
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

	private void doDeploy(String path, HostBean host, ProcessorBean proc, File file, Integer id) {
		service.execute(() -> {
			ChannelSftp ch = null;
			ChannelExec channel = null;
			ConnectManager connMgr = getConnMgr();
			try {
				String destFile = String.format("%s/%s", proc.getPath(),file.getName());
				updateStatus(id, proc, "正在连接服务器");
				Session session = createSession(host);
				updateStatus(id, proc, "开始上传文件");
				ch = (ChannelSftp) session.openChannel("sftp");
				ch.connect(1000 * 120);
				ch.put(file.getAbsolutePath(), destFile);
				LOG.info("scp:{} to:{}",file,destFile);

				updateStatus(id, proc, "文件上传成功,准备重启");
				String cmd = String.format("cd %s;./service.sh all", path);
				LOG.info("upload file success,will exe:{}", cmd);
				channel = (ChannelExec) session.openChannel("exec");
				List<String> exe = new LinkedList<>();
				connMgr.doExeWithProcess(host.getName(), cmd, session, new IExeCallBack() {

					@Override
					public void onResponse(String line) {
						pushLog2Queue(proc, line, "");
						exe.add(line);
						exe.add("\n");
					}

					@Override
					public void onError(String err) {
						if (err.length() > 0)
							pushLog2Queue(proc, err, "请检查工程目录是否有lock");
						// updateStatus(id, proc, "部署失败", "请检查工程目录是否有lock");
					}
				});

				String log = exe.toString();
				if (log.length() > 6000) {
					log = log.substring(0, 6000);
				}
				updateStatus(id, proc, "部署完成", log);
			} catch (Throwable e) {
				LOG.error("error", e);
				StringWriter s = new StringWriter();
				e.printStackTrace(new PrintWriter(s));
				updateStatus(id, proc, "部署失败,内部错误", s.toString());
			} finally {
				logMap.remove(proc.getId());
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

	private void pushLog2Queue(ProcessorBean proc, String status, String log) {
		Integer pid = proc.getId();
		BlockingQueue<String> queue = logMap.get(pid);
		if (queue == null) {
			synchronized (logMap) {
				if (!logMap.containsKey(pid)) {
					logMap.put(pid, new LinkedBlockingQueue<>(10000));
					logMap.notifyAll();
				}
			}
		}
		queue = logMap.get(pid);
		if (status.length() > 0)
			queue.add(status);
		if (log.length() > 0)
			queue.add(log);
	}

	private DeployRecord updateStatus(Integer id, ProcessorBean proc, String status, String log) {
		pushLog2Queue(proc, status, log);
		DeployRecord record = new DeployRecord();
		record.setStatus(status);
		record.setLog(log);
		record.setId(id);
		dDao.updateByPrimaryKeySelective(record);
		return record;
	}

	private DeployRecord updateStatus(Integer id, ProcessorBean proc, String status) {
		return updateStatus(id, proc, status, "");
	}
}
