package com.czp.ulc.web.cmd;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

/**
 * 命令处理器管理类,自动扫描cmdhandler
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月10日</li>
 * 
 * @version 0.0.1
 */
@Service
public class CmdHandlerManager implements ApplicationListener<ContextRefreshedEvent> {

	/** 处理器map */
	private ConcurrentHashMap<String, ICommandHandler> cmdHandler = new ConcurrentHashMap<>();

	/**
	 * 注册处理器
	 * 
	 * @param handler
	 * @return
	 */
	public boolean registHandler(ICommandHandler handler) {
		return cmdHandler.put(handler.cmd(), handler) != null;
	}

	/**
	 * 获取指定类型的处理器
	 * @param cmd
	 * @return
	 */
	public ICommandHandler getHandler(String cmd) {
		return cmdHandler.get(cmd);
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		Map<String, ICommandHandler> map = event.getApplicationContext().getBeansOfType(ICommandHandler.class);
		for (Entry<String, ICommandHandler> entry : map.entrySet()) {
			registHandler(entry.getValue());
		}
	}

}
