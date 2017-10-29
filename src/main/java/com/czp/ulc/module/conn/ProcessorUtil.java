package com.czp.ulc.module.conn;

/**
 * @dec Function
 * @author coder_czp@126.com
 * @date 2017年10月28日/上午9:41:44
 * @copyright coder_czp@126.com
 *
 */
public class ProcessorUtil {

	public static String getProc(String host, String file) {
		if (file.startsWith("/var/www/data/work/")) {
			// /var/www/data/work/itrip_mobile/tomcat7/logs/request.log
			return file.split("/")[5];
		}
		if (file.startsWith("/data/work/itrip_")) {
			// /data/work/itrip_3/itrip_mobile/tomcat7/logs/request.log
			return file.split("/")[3];
		}
		throw new RuntimeException("can't find processor name from " + file);
	}
}
