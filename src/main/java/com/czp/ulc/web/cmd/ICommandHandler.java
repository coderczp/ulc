package com.czp.ulc.web.cmd;

import com.czp.ulc.core.bean.HostBean;
import com.czp.ulc.core.bean.ProcessorBean;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月30日</li>
 * 
 * @version 0.0.1
 */

public interface ICommandHandler {

	String cmd();
	
	/***
	 * 在指定的机器执行命令
	 * @param cmd
	 * @param proc
	 * @param host
	 * @return
	 */
	String handler(String cmd, ProcessorBean proc, HostBean host);

}
