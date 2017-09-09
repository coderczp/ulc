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
import java.util.Set;

import org.apache.ibatis.jdbc.SQL;
import org.springframework.util.Assert;

import com.czp.ulc.common.bean.HostBean;
import com.czp.ulc.common.bean.IndexMeta;
import com.czp.ulc.common.bean.KeywordRule;
import com.czp.ulc.common.bean.MonitorConfig;
import com.czp.ulc.common.param.QueryParam;
import com.czp.ulc.common.util.Utils;

/**
 * Function:动态构建SQL
 *
 * @date:2017年3月27日/下午5:13:14
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class DynamicSql {

	public String queryLuceneFile(QueryParam param) {
		StringBuilder sql = new StringBuilder("select * from lucene_file where ");
		sql.append(" itime >=").append(param.from);
		sql.append(" AND itime <=").append(param.to);
		Set<String> sers = param.servers;
		if (sers != null && sers.size() > 0) {
			int len = param.servers.size();
			sql.append(" AND server in(");
			for (String ser : sers) {
				sql.append("'").append(ser).append("'");
				if (--len > 0) {
					sql.append(",");
				}
			}
			sql.append(")");
		}
		 System.out.println(sql);
		return sql.toString();
	}

	public String addIndexMeta(IndexMeta meta) {
		String update = String.format(
				"UPDATE index_meta set bytes=bytes+%s,`lines`=+`lines`+%s,docs=docs+%s where id=0;", meta.getBytes(),
				meta.getLines(), meta.getDocs());
		return String.format("INSERT INTO index_meta (bytes,`lines`,docs,shard_id) VALUES(%s,%s,%s,%s);%s",
				meta.getBytes(), meta.getLines(), meta.getDocs(), meta.getShardId(), update);

	}

	public String queryAllRecord(Integer size) {
		return "select id,project,host,author,status,time from deploy_record order by id desc limit " + size;
	}

	public String selectRecordLog(Integer id) {
		return "select log from deploy_record where id= " + id;
	}

	/***
	 * 查询指定分片的indexMeta总数
	 * 
	 * @param shardId
	 * @return
	 */
	public String countIndexMeta(Integer shardId) {
		// 第0行是汇总行,mysql定时汇总
		if (shardId == null)
			return "select bytes,`lines`,docs from index_meta where id=0";
		return "select sum(bytes) as bytes,sum(`lines`) as `lines`,sum(docs) as docs from index_meta where id>0 and shardId="
				+ shardId;
	}

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

	public String listMonitorFile(final MonitorConfig arg) {
		return new SQL() {
			{
				SELECT("*");
				FROM("monitor_config");
				WHERE("1=1");
				if (Utils.notEmpty(arg.getHostId()))
					WHERE("hostId=#{hostId}");
			}
		}.toString();
	}
}
