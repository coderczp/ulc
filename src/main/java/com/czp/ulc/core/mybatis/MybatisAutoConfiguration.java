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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.github.pagehelper.PageHelper;

/**
 * Function:mybatis配置
 *
 * @date:2017年3月27日/下午4:43:25
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
@Configuration
@ConditionalOnClass({ SqlSessionFactory.class, SqlSessionFactoryBean.class })
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class MybatisAutoConfiguration {

	@Autowired
	private Environment env;

	@Autowired(required = false)
	private Interceptor[] interceptors;

	@Autowired
	private ResourceLoader resourceLoader;// = new DefaultResourceLoader();

	@Bean(name = "sqlSessionFactory")
	@ConditionalOnMissingBean
	public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {

		SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
		factory.setDataSource(dataSource);

		if (this.interceptors != null && this.interceptors.length > 0) {
			factory.setPlugins(this.interceptors);
		}

		String configPath = env.getProperty("mybatis.config.path");
		String aliases = env.getProperty("mybatis.type.aliases.package");
		String handler = env.getProperty("mybatis.type.handlers.package");

		if (StringUtils.hasText(configPath)) {
			factory.setConfigLocation(getResource(configPath));
		}
		if (StringUtils.hasText(aliases)) {
			factory.setTypeAliasesPackage(aliases);
		}
		if (StringUtils.hasText(handler)) {
			factory.setTypeAliasesPackage(handler);
		}
		factory.setMapperLocations(getMappers());
		return factory.getObject();
	}

	@Bean(name = "dataSource")
	public DataSource creatDatasource() throws Exception {
		String dbConfigPath = env.getProperty("db.config.path");
		return DruidDataSourceFactory.createDataSource(loadConfig(dbConfigPath));
	}

	private Resource[] getMappers() throws Exception {
		File file = new File(env.getProperty("mybatis.mapper.path"));
		if (file.isFile())
			return new Resource[] { new FileSystemResource(file.getAbsolutePath()) };

		File[] files = file.getParentFile().listFiles();
		Resource[] resources = new Resource[files.length];
		for (int i = 0; i < resources.length; i++) {
			resources[i] = new FileSystemResource(files[i].getAbsolutePath());
		}
		return resources;
	}

	private Resource getResource(String path) {
		return this.resourceLoader.getResource(path);
	}

	private Properties loadConfig(String path) throws IOException {
		// PathMatchingResourcePatternResolver resolver = new
		// PathMatchingResourcePatternResolver();
		FileSystemResource res = new FileSystemResource(path);
		Properties config = new Properties();
		config.load(res.getInputStream());
		return config;
	}

	@Bean
	@ConditionalOnMissingBean
	public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionTemplate(sqlSessionFactory, ExecutorType.SIMPLE);
	}

	/**
	 * 分页插件
	 *
	 * @param dataSource
	 * @return
	 */
	@Bean
	public PageHelper pageHelper() {
		PageHelper pageHelper = new PageHelper();
		Properties p = new Properties();
		p.setProperty("offsetAsPageNum", "true");
		p.setProperty("rowBoundsWithCount", "true");
		p.setProperty("reasonable", "true");
		pageHelper.setProperties(p);
		return pageHelper;
	}
}
