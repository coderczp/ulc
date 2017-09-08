package com.czp.ulc.common.dao;

import java.util.List;

import org.apache.ibatis.annotations.SelectProvider;

import com.czp.ulc.common.bean.DeployRecord;
import com.czp.ulc.common.mybatis.BaseDao;
import com.czp.ulc.common.mybatis.DynamicSql;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年8月9日 下午4:27:28</li>
 * 
 * @version 0.0.1
 */

public interface IDeployRecordDao extends BaseDao<DeployRecord> {

	@SelectProvider(type = DynamicSql.class, method = "queryAllRecord")
	List<DeployRecord> queryAll(int size);

	@SelectProvider(type = DynamicSql.class, method = "selectRecordLog")
	String selectLog(int id);

}
