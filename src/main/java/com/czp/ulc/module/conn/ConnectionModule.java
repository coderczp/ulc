package com.czp.ulc.module.conn;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.czp.ulc.core.dao.HostDao;
import com.czp.ulc.core.dao.KeywordRuleDao;
import com.czp.ulc.core.dao.MonitoConfigDao;
import com.czp.ulc.core.message.MessageCenter;
import com.czp.ulc.core.zk.ZkManager;
import com.czp.ulc.module.IModule;
import com.czp.ulc.module.alarm.AlarmSender;

/**
 * 链接管理模块
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月11日 下午4:10:34</li>
 * 
 * @version 0.0.1
 */
@Service
public class ConnectionModule implements IModule {

	@Autowired
	private HostDao hostDao;

	@Autowired
	private Environment env;

	@Autowired
	private MonitoConfigDao cfgDao;

	@Autowired
	private MessageCenter mqCenter;

	@Autowired
	private KeywordRuleDao keyDao;

	@Autowired
	private ZkManager zkManager;

	private ConnectionManager conMgr;

	@Override
	public boolean start(SingletonBeanRegistry ctx) {

		if (zkManager.isClusterModel()) {
			String port = env.getProperty("server.port");
			Objects.requireNonNull(port, "server.port not found");
			conMgr = new ClusterConnManager(port, zkManager);
		} else {
			conMgr = new ConnectionManager();
		}

		conMgr.setCfgDao(cfgDao);
		conMgr.setHostDao(hostDao);
		conMgr.setMqCenter(mqCenter);

		mqCenter.addConcumer(conMgr);
		mqCenter.addConcumer(AlarmSender.getInstance());
		mqCenter.addConcumer(new ErrorLogHandler(keyDao, mqCenter));

		ctx.registerSingleton("connectManager", conMgr);
		conMgr.onStart();

		return true;
	}

	@Override
	public boolean stop() {
		return false;
	}

	@Override
	public String name() {
		return "Connection Manager Module";
	}

	@Override
	public int order() {
		return 1;
	}

}
