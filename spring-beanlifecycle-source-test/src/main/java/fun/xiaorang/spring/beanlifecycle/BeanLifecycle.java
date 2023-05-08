package fun.xiaorang.spring.beanlifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/6 16:50
 */
public class BeanLifecycle implements BeanNameAware, BeanFactoryAware, BeanClassLoaderAware, ApplicationContextAware, InitializingBean, DisposableBean {
	private static final Logger LOGGER = LoggerFactory.getLogger(BeanLifecycle.class);
	private Integer id;

	public BeanLifecycle() {
		LOGGER.info("constructor");
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		LOGGER.info("property setId() => {}", id);
		this.id = id;
	}

	@Override
	public void setBeanName(String name) {
		LOGGER.info("BeanNameAware#setBeanName() => {}", name);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		LOGGER.info("BeanFactoryAware#setBeanFactory() => {}", beanFactory);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		LOGGER.info("BeanClassLoaderAware#setBeanClassLoader() => {}", classLoader);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		LOGGER.info("ApplicationContextAware#setApplicationContext() => {}", applicationContext);
	}

	@PostConstruct
	public void postConstruct() {
		LOGGER.info("postConstruct()");
	}

	@Override
	public void afterPropertiesSet() {
		LOGGER.info("InitializingBean#afterPropertiesSet()");
	}

	public void initMethod() {
		LOGGER.info("initMethod()");
	}

	@PreDestroy
	public void preDestroy() {
		LOGGER.info("preDestroy()");
	}

	@Override
	public void destroy() {
		LOGGER.info("DisposableBean#destroy()");
	}

	public void destroyMethod() {
		LOGGER.info("destroyMethod()");
	}

	@Autowired
	public void autowire(@Value("${spring.profiles.active}") String activeProfile) {
		LOGGER.info("autowire: {}", activeProfile);
	}

	@Resource
	public void resource(@Value("${JAVA_HOME}") String home) {
		LOGGER.info("resource: {}", home);
	}
}
