package com.czp.ulc.common.module;

import org.springframework.beans.factory.config.SingletonBeanRegistry;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月11日 下午4:11:23</li>
 * 
 * @version 0.0.1
 */

public interface IModule {

	/***
	 * 启动整个模块
	 * @return
	 */
	boolean start(SingletonBeanRegistry ctx);

	/***
	 * 停止模块
	 * @return
	 */
	boolean stop();

	/***
	 * 模块名
	 * @return
	 */
	String name();
}
