/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.test;

import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Test;

import com.czp.ulc.core.bean.HostBean;
import com.czp.ulc.core.bean.KeywordRule;
import com.czp.ulc.core.bean.MonitorConfig;
import com.czp.ulc.core.mybatis.DynamicSql;

/**
 * Function:测试SQL生成器
 *
 * @date:2017年3月28日/下午4:03:47
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class SQLBuilderTest {

	protected DynamicSql dynamicSql = new DynamicSql();

	@Test
	public void test() {
		HostBean param = new HostBean();
		param.setId(123);
		param.setUser("user");
		String sql = dynamicSql.updateHost(param);
		System.out.println(sql);
		assertTrue("create updateHost sql error", sql.contains("user="));
	}

	@Test
	public void testSql2() {
		KeywordRule rule = new KeywordRule();
		rule.setHostId(1);
		rule.setFile("./gc.log");
		rule.setKeyword("Exception");
		String sql = dynamicSql.listKeywordRule(rule);
		System.out.println(sql);
		assertTrue("create listKeywordRule sql error", sql.contains("file like"));
	}

	@Test
	public void testSql3() {
		MonitorConfig rule = new MonitorConfig();
		rule.setHostId(1);
		rule.setFile("./gc.log");
		String sql = dynamicSql.listMonitorFile(rule);
		System.out.println(sql);
		assertTrue("create listKeywordRule sql error", sql.contains("hostId="));
	}

	public static void writeZipFile(String file) throws Exception {
		long st = System.currentTimeMillis();
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file + ".zip"));
		ZipEntry ze = new ZipEntry(file);
		zos.putNextEntry(ze);
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		byte[] buf = new byte[1024];
		int len = 0;
		while ((len = bis.read(buf)) != -1) {
			zos.write(buf, 0, len);
		}
		bis.close();
		zos.close();
		System.out.println(System.currentTimeMillis() - st);
	}
}
