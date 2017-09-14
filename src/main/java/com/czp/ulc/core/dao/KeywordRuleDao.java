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

import org.apache.ibatis.annotations.SelectProvider;

import com.czp.ulc.core.bean.KeywordRule;
import com.czp.ulc.core.mybatis.BaseDao;
import com.czp.ulc.core.mybatis.DynamicSql;

/**
 * Function:KeywordRule DAO
 *
 * @date:2017年3月28日/下午5:06:15
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public interface KeywordRuleDao extends BaseDao<KeywordRule> {

	/***
	 * 查找适合当前主机和文件的规则,返回 rule.file=file
	 * 
	 * @param file
	 * @return
	 */
	@SelectProvider(type = DynamicSql.class, method = "listKeywordRule")
	List<KeywordRule> list(KeywordRule rule);

}
