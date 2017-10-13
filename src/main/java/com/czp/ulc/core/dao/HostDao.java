/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.core.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.SelectProvider;

import com.czp.ulc.core.bean.HostBean;
import com.czp.ulc.core.mybatis.BaseDao;
import com.czp.ulc.core.mybatis.DynamicSql;

/**
 * Function:主机信息管理类
 *
 * @date:2017年3月27日/下午4:08:20
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public interface HostDao extends BaseDao<HostBean> {

	@SelectProvider(type = DynamicSql.class, method = "listHost")
	List<HostBean> list(Map<String, Object> param);

	@SelectProvider(type = DynamicSql.class, method = "getHostById")
	HostBean get(Integer id);

	@SelectProvider(type = DynamicSql.class, method = "listSpecHost")
	List<HostBean> listSpec(String cols,HostBean param);
}
