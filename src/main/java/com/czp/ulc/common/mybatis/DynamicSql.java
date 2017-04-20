/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.common.mybatis;

import java.util.Map;

import org.apache.ibatis.jdbc.SQL;
import org.springframework.util.Assert;

import com.czp.ulc.common.bean.HostBean;
import com.czp.ulc.common.bean.KeywordRule;
import com.czp.ulc.common.bean.MonitorFile;
import com.czp.ulc.common.util.Utils;

/**
 * Function:动态构建SQL
 *
 * @date:2017年3月27日/下午5:13:14
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class DynamicSql {

	/***
	 * 查询主机信息
	 * 
	 * @param param
	 * @return
	 */
	public String listHost(Map<String, Object> param) {
		if (param == null || param.isEmpty())
			return "SELECT * FROM host_bean";
		else
			return "SELECT * FROM host_bean";
	}

	/**
	 * 更新主机信息
	 * 
	 * @param param
	 * @return
	 */
	public String updateHost(final HostBean param) {
		Assert.notNull(param.getId(), "id is required where update");
		return new SQL() {
			{
				UPDATE("host_bean");
				SET("id=id");
				if (param.getPort() != HostBean.DEFAULT_PORT)
					SET("port=#{port}");
				if (Utils.notEmpty(param.getUser()))
					SET("user=#{user}");
				if (Utils.notEmpty(param.getPwd()))
					SET("pwd=#{pwd}");
				WHERE("ID=#{id}");
			}
		}.toString();
	}

	public String listKeywordRule(final KeywordRule rule) {
		return new SQL() {
			{
				SELECT("*");
				FROM("keyword_rule");
				WHERE("1=1");
				if (Utils.notEmpty(rule.getFile())) {
					String file = rule.getFile();
					String name = file.substring(file.lastIndexOf("/") + 1);
					WHERE("file like '%" + name + "'");
				}
				if (Utils.notEmpty(rule.getHostId()))
					WHERE("hostId=#{hostId}");
				if (Utils.notEmpty(rule.getKeyword()))
					WHERE("keyword like '%" + rule.getKeyword() + "%'");
			}
		}.toString();
	}

	public String listMonitorFile(final MonitorFile arg) {
		return new SQL() {
			{
				SELECT("*");
				FROM("monitor_file");
				WHERE("1=1");
				if (Utils.notEmpty(arg.getHostId()))
					WHERE("hostId=#{hostId}");
			}
		}.toString();
	}
}
