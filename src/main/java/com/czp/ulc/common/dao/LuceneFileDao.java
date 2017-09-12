package com.czp.ulc.common.dao;

import java.util.List;

import org.apache.ibatis.annotations.SelectProvider;

import com.czp.ulc.common.bean.LuceneFile;
import com.czp.ulc.common.mybatis.BaseDao;
import com.czp.ulc.common.mybatis.DynamicSql;
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
}
