package com.czp.ulc.web;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.czp.ulc.common.bean.DeployRecord;
import com.czp.ulc.common.bean.HostBean;
import com.czp.ulc.common.dao.HostDao;
import com.czp.ulc.common.dao.IDeployRecordDao;
import com.czp.ulc.module.conn.ConnectManager;
import com.czp.ulc.util.MiniHeap;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;

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
	private IDeployRecordDao dDao;

	@Autowired
	private ApplicationContext context;

	private ExecutorService service = Executors.newFixedThreadPool(4);

	private static final Logger LOG = LoggerFactory.getLogger(WebErrorHandler.class);

	@PreDestroy
	public void stop() {
		service.shutdown();
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
	public Object listtar(int size) {
		FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".tar");
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

	@PostMapping("/deploy")
	public Object deploy(HttpServletRequest req, @RequestParam String tar, @RequestParam String hostId,
			@RequestParam String path) {

		JSONObject json = new JSONObject();

		File tarFile = FileUploadConstroller.getPath(tar);
		if (!tarFile.exists()) {
			json.put("code", 400);
			json.put("err", tar + " not exist,please upload frist");
			return json;
		}
		HostBean tmp = new HostBean();
		tmp.setName(hostId);
		HostBean host = dao.selectOne(tmp);
		if (host == null) {
			json.put("code", 400);
			json.put("err", "host not exist,please check");
			return json;
		}

		try {
			HttpSession session = req.getSession();
			String user = session.getAttribute("user").toString();

			String file = tarFile.getAbsolutePath();
			String projectName = tar.substring(tar.indexOf("_") + 1);
			String destFile = String.format("%s/%s", path, projectName);
			SimpleDateFormat spf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

			DeployRecord record = new DeployRecord();
			record.setAuthor(user);
			record.setTime(spf.format(new Date()));
			record.setHost(host.getName());
			record.setProject(projectName);
			record.setStatus("准备上传文件");

			int res = dDao.insertUseGeneratedKeys(record);
			if (res > 0) {
				doDeploy(path, host, file, destFile, record.getId());
			} else {
				throw new RuntimeException("添加部署记录失败,请重试");
			}
			json.put("code", 200);
			json.put("id", record.getId());
		} catch (Exception e) {
			LOG.error("error", e);
			json.put("code", 500);
			json.put("err", e.getMessage());
		}
		return json;
	}

	private void doDeploy(String path, HostBean host, String file, String destFile, Integer id) {
		service.execute(() -> {
			ChannelSftp ch = null;
			try {
				updateStatus(id, "开始上传文件");
				ch = createStfp(host);
				ch.put(file, destFile);
				ch.disconnect();

				updateStatus(id, "文件上传,准备重启");
				LOG.info("upload file success");
				String cmd = String.format("cd %s;./service.sh all", path);
				List<String> exe = getConnMgr().exe(host.getName(), cmd);
				if (exe.isEmpty()) {
					updateStatus(id, "部署失败", "请检查工程目录是否有lock");
				} else {
					String log = exe.toString();
					updateStatus(id, "部署完成", log);
				}
			} catch (Exception e) {
				LOG.error("error", e);
				updateStatus(id, "部署失败,连接超时");
			} finally {
				if (ch != null)
					ch.disconnect();
			}
		});
	}

	private ChannelSftp createStfp(HostBean host) throws JSchException {
		ChannelSftp ch = null;
		try {
			ch = (ChannelSftp) getConnMgr().openChannel(host, "sftp");
			ch.connect(1000 * 120);
		} catch (JSchException ex) {
			ex.printStackTrace();
			getConnMgr().disconnect(host);
			ch = (ChannelSftp) getConnMgr().openChannel(host, "sftp");
			ch.connect(1000 * 120);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ch;
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
