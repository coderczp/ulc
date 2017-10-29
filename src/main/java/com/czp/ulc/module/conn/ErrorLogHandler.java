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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.core.DaemonTimer;
import com.czp.ulc.core.bean.HostBean;
import com.czp.ulc.core.bean.KeywordRule;
import com.czp.ulc.core.dao.KeywordRuleDao;
import com.czp.ulc.core.message.Message;
import com.czp.ulc.core.message.MessageCenter;
import com.czp.ulc.core.message.MessageListener;
import com.czp.ulc.module.alarm.AlarmBean;
import com.czp.ulc.util.Utils;

/**
 * Function:提取错误日志,如果匹配关键字,则执行相应的操作
 *
 * @date:2017年3月27日/下午2:52:47
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class ErrorLogHandler implements MessageListener<ReadResult>, Runnable {

	private class MutilLineBean {
		AtomicInteger lineCount = new AtomicInteger();
		StringBuffer lines = new StringBuffer();
		String lastFile;
		HostBean host;

		 @SuppressWarnings("unused")
		MutilLineBean(HostBean host, String lastFile) {
			this.lastFile = lastFile;
			this.host = host;
		}

	}

	private int lineCount = 50;
	private KeywordRuleDao kwDao;
	private MessageCenter mqCenter;
	private static final Logger LOG = LoggerFactory.getLogger(ErrorLogHandler.class);
	private ConcurrentHashMap<String, MutilLineBean> map = new ConcurrentHashMap<String, MutilLineBean>();

	public ErrorLogHandler(KeywordRuleDao kwDao, MessageCenter mqCenter) {
		this.kwDao = kwDao;
		this.mqCenter = mqCenter;
		DaemonTimer.getInstance().addTask(this, 5000);
	}

	@Override
	public void onExit() {

	}

	@Override
	public boolean onMessage(ReadResult event, Map<String, Object> ext) {
		// String file = event.getFile();
		// if (!file.endsWith("error.log") && !file.endsWith(".out")) {
		// return false;
		// }
		//
		// String serName = event.getHost().getName();
		// String hostFile = serName.concat("#").concat(file);
		// if (!map.containsKey(hostFile)) {
		// map.putIfAbsent(hostFile, new MutilLineBean(event.getHost(), file));
		// }
		//
		// MutilLineBean bean = map.get(hostFile);
		// synchronized (bean) {
		// bean.lines.append(event.getLine()).append("<br>\n");
		// bean.lineCount.getAndIncrement();
		// }
		//
		// /** 每lineCount行开始检测是否又关键字出现,为了防止一直<50行并且有错误不报,定时器会检测 */
		// if (bean.lineCount.get() > lineCount) {
		// processIfMatch(bean);
		// resetCache(bean);
		// }

		return false;
	}

	@Override
	public void run() {
		for (Entry<String, MutilLineBean> entry : map.entrySet()) {
			MutilLineBean bean = entry.getValue();
			processIfMatch(bean);
			resetCache(bean);
		}
	}

	@Override
	public Class<ReadResult> processClass() {
		return ReadResult.class;
	}

	private void processIfMatch(MutilLineBean bean) {
		if (bean.lineCount.get() == 0) {
			return;
		}

		KeywordRule temp = new KeywordRule();
		temp.setFile(bean.lastFile);
		List<KeywordRule> rules = kwDao.list(temp);

		String tmpLines = null;
		StringBuffer lines = bean.lines;
		for (KeywordRule rule : rules) {

			if (!bean.lastFile.contains(rule.getFile()))
				continue;

			// 检查是否匹配排除规则
			if (Utils.notEmpty(rule.getExclude()) && lines.indexOf(rule.getExclude()) > -1) {
				LOG.info("match exclude:{}\ncontent:{}", rule.getExclude(), lines);
				continue;
			}

			// 是否包含关键字
			if (lines.indexOf(rule.getKeyword()) == -1)
				continue;

			if (tmpLines == null)
				tmpLines = lines.toString();

			AlarmBean alarm = new AlarmBean(bean.host, bean.lastFile, tmpLines, AlarmBean.Type.EMAIL);
			mqCenter.push(new Message(alarm));
		}
	}

	private void resetCache(MutilLineBean bean) {
		synchronized (bean) {
			if (bean.lineCount.get() > 0) {
				bean.lines = new StringBuffer();
				bean.lineCount.set(0);
			}
		}
	}

}
