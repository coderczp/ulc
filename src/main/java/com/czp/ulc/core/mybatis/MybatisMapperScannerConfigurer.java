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

import java.util.Properties;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import tk.mybatis.spring.mapper.MapperScannerConfigurer;

/**
 * Function:XXX TODO add desc
 *
 * @date:2017年3月28日/上午9:34:39
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
@Configuration
@AutoConfigureAfter(MybatisAutoConfiguration.class)
public class MybatisMapperScannerConfigurer {

	@Bean
	public MapperScannerConfigurer mapperScannerConfigurer(Environment env) {
		MapperScannerConfigurer config = new MapperScannerConfigurer();
		config.setBasePackage(env.getProperty("mybatis.type.dao.package"));
		config.setSqlSessionFactoryBeanName("sqlSessionFactory");

		Properties properties = new Properties();
		// 不要把MyMapper放到 basePackage中，也就是不能同其他Mapper一样被扫描到。
		properties.setProperty("mappers", BaseDao.class.getName());
		properties.setProperty("notEmpty", "false");
		properties.setProperty("IDENTITY", "MYSQL");
		config.setProperties(properties);
		return config;
	}
}
