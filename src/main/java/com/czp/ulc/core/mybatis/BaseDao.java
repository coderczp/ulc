/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.core.mybatis;

import tk.mybatis.mapper.common.Mapper;
import tk.mybatis.mapper.common.MySqlMapper;

/**
 * Function:通用mapper 该接口不能被扫描到，否则会出错
 * 
 * @date:2017年3月27日/下午4:40:42
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public interface BaseDao<T> extends Mapper<T>, MySqlMapper<T> {
  
}
