package com.czp.ulc.common.dao;

import java.util.List;

import org.apache.ibatis.annotations.SelectProvider;

import com.czp.ulc.common.bean.MonitorFile;
import com.czp.ulc.common.mybatis.BaseDao;
import com.czp.ulc.common.mybatis.DynamicSql;

/**
 * 监控文件的dao
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年3月30日 下午4:07:47</li>
 * 
 * @version 0.0.1
 */

public interface MonitoFileDao extends BaseDao<MonitorFile> {

	/***
	 * 查找监控文件
	 * 
	 * @param file
	 * @return
	 */
	@SelectProvider(type = DynamicSql.class, method = "listMonitorFile")
	List<MonitorFile> list(MonitorFile arg);
}
