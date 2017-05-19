package com.czp.ulc.common.meta;

import java.io.File;
import java.io.IOException;

import com.czp.ulc.common.shutdown.ShutdownCallback;

/**
 * 支持滚动写文件
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月18日 上午9:20:27</li>
 * 
 * @version 0.0.1
 */

public interface RollingWriter extends AutoCloseable, ShutdownCallback {

	File getCurrentFile();

	File[] getAllFiles();

	boolean isHistoryFile(File file);

	long append(byte[] bytes) throws IOException;

}
