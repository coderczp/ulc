/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.main;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.czp.ulc.common.shutdown.ShutdownCallback;
import com.czp.ulc.module.IModule;
import com.czp.ulc.web.AccessFilter;

/**
 * Function:程序入口
 *
 * @date:2017年3月24日/下午2:16:04
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */

@EnableAsync
@EnableAutoConfiguration
@ComponentScan(value = { "com.czp.ulc" })
public class Application extends WebMvcConfigurerAdapter
		implements ShutdownCallback, BeanDefinitionRegistryPostProcessor, ApplicationListener<ApplicationContextEvent> {

	private Environment envBean;
	private TreeSet<IModule> modules = new TreeSet<>();

	private static ConfigurableListableBeanFactory context;
	private static Logger LOG = LoggerFactory.getLogger(Application.class);

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String resDir = envBean.getProperty("res.dir.static");
		String staticFile = String.format("file:%s", resDir);
		registry.addResourceHandler("/**").addResourceLocations(staticFile);
		super.addResourceHandlers(registry);
	}

	@Bean
	public FilterRegistrationBean dawsonApiFilter() {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		String loginUrl = envBean.getProperty("login.url.itrip");
		String skipUrl = envBean.getProperty("login.check.skip.url");
		String key = envBean.getProperty("login.decrypt.key");
		registration.setFilter(new AccessFilter(loginUrl, skipUrl, key));
		registration.addUrlPatterns("/*");
		return registration;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory arg0) throws BeansException {
		envBean = arg0.getBean(Environment.class);
		mergeProperties(envBean);
		context = arg0;
	}

	public static <T> T getBean(Class<T> cls) {
		return context.getBean(cls);
	}

	/**
	 * 把所有的配置merge到Environment
	 * 
	 * @param envBean
	 * 
	 * @param arg0
	 */
	private void mergeProperties(Environment envBean) {
		try {
			String resDir = envBean.getProperty("res.dir");
			FileSystemResource res = new FileSystemResource(resDir);
			File file = res.getFile();
			File[] listFiles = file.listFiles();
			Properties config = new Properties();
			for (File item : listFiles) {
				if (!item.toString().endsWith(".properties"))
					continue;
				FileSystemResource resTmp = new FileSystemResource(item);
				InputStream is = resTmp.getInputStream();
				config.load(is);
				is.close();
			}

			ConfigurableEnvironment env = (ConfigurableEnvironment) envBean;
			Map<String, Object> map = env.getSystemProperties();
			for (Entry<Object, Object> entry : config.entrySet()) {
				map.put(entry.getKey().toString(), entry.getValue());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry arg0) throws BeansException {
	}

	@Override
	public void onApplicationEvent(ApplicationContextEvent event) {
		modules = sortByOrder(context.getBeansOfType(IModule.class));
		if (event instanceof ContextRefreshedEvent) {
			startModule();
		} else if (event instanceof ContextStoppedEvent) {
			stopModule();
		}
	}

	private TreeSet<IModule> sortByOrder(Map<String, IModule> beans) {
		TreeSet<IModule> set = new TreeSet<IModule>(IModule.COMPER);
		set.addAll(beans.values());
		return set;
	}

	private synchronized void stopModule() {
		for (IModule iModule : modules) {
			String moduleName = iModule.name();
			LOG.info("moudle:[{}] stoping", moduleName);
			boolean start = iModule.stop();
			LOG.info("moudle:[{}],stoped:{}", moduleName, start);
		}
		modules.clear();
	}

	private void startModule() {
		for (IModule iModule : modules) {
			String moduleName = iModule.name();
			LOG.info("moudle:[{}] starting", moduleName);
			boolean start = iModule.start(context);
			LOG.info("moudle:[{}],start:{}", moduleName, start);
		}
	}

	@Override
	public void onSystemExit() {
		if (context == null)
			return;
		stopModule();

	}

	public static void main(String[] args) throws URISyntaxException {
		if (args.length < 1) {
			System.out.println("usage: java -jar app.jar --spring.config.location=xx/application.properties");
			return;
		}
		SpringApplication.run(Application.class, args);
	}
}
