package com.czp.ulc.common.module.conn;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.stereotype.Service;

import com.czp.ulc.collect.ConnectManager;
import com.czp.ulc.collect.RemoteLogCollector;
import com.czp.ulc.common.ThreadPools;
import com.czp.ulc.common.bean.HostBean;
import com.czp.ulc.common.dao.HostDao;
import com.czp.ulc.common.dao.MonitoConfigDao;
import com.czp.ulc.common.module.IModule;

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
	private MonitoConfigDao mDao;

	private static Logger LOG = LoggerFactory.getLogger(ConnectionModule.class);

	@Override
	public boolean start(SingletonBeanRegistry ctx) {
		ThreadPools.getInstance().startThread("conn-moudle-start", this, true);
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
		List<HostBean> hosts = hostDao.list(null);
		for (HostBean host : hosts) {
			try {
				ConnectManager.getInstance().connect(host);
				RemoteLogCollector.monitorIfNotExist(host, mDao);
			} catch (Exception e) {
				LOG.info("connect err:" + host, e);
			}
		}
	}

}
