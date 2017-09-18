package com.czp.ulc.core.dao;

import java.util.List;

import org.apache.ibatis.annotations.SelectProvider;

import com.czp.ulc.core.bean.Menu;
import com.czp.ulc.core.bean.UserMenu;
import com.czp.ulc.core.mybatis.BaseDao;
import com.czp.ulc.core.mybatis.DynamicSql;

/**
 * 请添加描述
 *
 * <li>创建人：coder_czp@126.com</li>
 * <li>创建时间：2017年9月18日 </li>
 * 
 * @version 0.0.1
 */
public interface UserMenuDao extends BaseDao<UserMenu>{

	@SelectProvider(type = DynamicSql.class, method = "queryMenus")
	List<Menu> query(UserMenu arg);

}
