package com.czp.ulc.core.shutdown;

/**
 * Function:系统退出回调
 *
 * @date:2016年6月8日/下午9:08:36
 * @Author:coder_czp@126.com
 * @version:1.0
 */
public interface ShutdownCallback {

	/***
	 * 退出执行
	 */
	void onSystemExit();
}
