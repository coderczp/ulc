package com.czp.ulc.core.dao;

import java.util.List;

import org.apache.ibatis.annotations.SelectProvider;

import com.czp.ulc.core.bean.LuceneFile;
import com.czp.ulc.core.mybatis.BaseDao;
import com.czp.ulc.core.mybatis.DynamicSql;
import com.czp.ulc.web.QueryCondtion;

/**
 * @dec 管理lucene文件
 * @author coder_czp@126.com
 * @date 2017年9月9日/上午10:10:58
 * @copyright coder_czp@126.com
 *
 */
public interface LuceneFileDao extends BaseDao<LuceneFile> {

	@SelectProvider(type = DynamicSql.class, method = "queryLuceneFile")
	List<LuceneFile> query(QueryCondtion param);
	
	/***
	 * 查询已经处理的最早的文件
	 * @param host
	 * @return
	 */
	@SelectProvider(type = DynamicSql.class, method = "queryEarliestFile")
	LuceneFile queryEarliestFile(String host);
}
