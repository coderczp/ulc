package com.czp.ulc.common.shutdown;

import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Function:统一管理系统推出事件
 *
 * @date:2016年6月8日/下午9:07:44
 * @Author:coder_czp@126.com
 * @version:1.0
 */
public class ShutdownManager extends Thread implements SignalHandler {

	private volatile boolean hasExecute = false;

	private static final ShutdownManager INSTANCE = new ShutdownManager();

	private Logger log = LoggerFactory.getLogger(ShutdownManager.class);

	private CopyOnWriteArraySet<ShutdownCallback> callbacks = new CopyOnWriteArraySet<ShutdownCallback>();

	private ShutdownManager() {
		registHook();
	}

	public static ShutdownManager getInstance() {
		return INSTANCE;
	}

	public void addCallback(ShutdownCallback callback) {
		callbacks.add(callback);
	}

	public CopyOnWriteArraySet<ShutdownCallback> getCallbacks() {
		return callbacks;
	}

	private void registHook() {
		// TERM（kill -15）、USR1（kill -10）、USR2（kill -12）
		// linux: SEGV, ILL, FPE, BUS, SYS, CPU, FSZ, ABRT, INT, TERM, HUP,
		// USR1, USR2, QUIT, BREAK, TRAP, PIPE
		// window:SEGV, ILL, FPE, ABRT, INT, TERM, BREAK
		try {
			String signalNames;
			String os = System.getProperty("os.name");
			if (os.toLowerCase().contains("windows")) {
				signalNames = "SEGV,ILL,ABRT,INT,TERM";
			} else {
				signalNames = "TERM,USR2";
			}
			for (String name : signalNames.split(",")) {
				Signal.handle(new Signal(name), this);
			}
			Runtime.getRuntime().addShutdownHook(this);
			log.info("success regist for:{}", signalNames);
		} catch (Exception e) {
			log.error("fill to regist signal", e);
		}
	}

	@Override
	public void run() {
		hasExecute = true;
		log.info("start call ShutdownCallback");
		for (ShutdownCallback call : callbacks) {
			try {
				call.onSystemExit();
				log.info("sucess to call:{}", call);
			} catch (Exception e) {
				log.error("fail to call:{}", call, e);
			}
		}
		log.info("success to call all ShutdownCallback");
	}

	@Override
	public void handle(Signal arg0) {
		log.info("recive signal:{}", arg0);
		if (!hasExecute)
			run();
	}

}
