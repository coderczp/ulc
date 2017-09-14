package com.czp.ulc.module.conn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.stereotype.Service;

import com.czp.ulc.core.ThreadPools;
import com.czp.ulc.core.bean.HostBean;
import com.czp.ulc.core.dao.HostDao;
import com.czp.ulc.core.dao.KeywordRuleDao;
import com.czp.ulc.core.dao.MonitoConfigDao;
import com.czp.ulc.core.message.MessageCenter;
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
public class ConnectionModule implements IModule, Runnable {

	@Autowired
	private HostDao hostDao;

	@Autowired
	private MonitoConfigDao cfgDao;

	@Autowired
	private MessageCenter mqCenter;

	@Autowired
	private KeywordRuleDao keyDao;

	private ConnectManager conMgr;

	private static Logger LOG = LoggerFactory.getLogger(ConnectionModule.class);

	@Override
	public boolean start(SingletonBeanRegistry ctx) {
		conMgr = new ConnectManager(mqCenter, cfgDao);
		mqCenter.addConcumer(AlarmSender.getInstance());
		mqCenter.addConcumer(new ErrorLogHandler(keyDao, mqCenter));

		ctx.registerSingleton("connectManager", conMgr);
		ThreadPools.getInstance().run("conn-moudle-start", this, true);
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
	public void run() {
		for (HostBean host : hostDao.list(null)) {
			try {
				conMgr.connect(host);
			} catch (Exception e) {
				LOG.info("connect err:" + host, e);
			}
		}
	}

	@Override
	public int order() {
		return 1;
	}

}
