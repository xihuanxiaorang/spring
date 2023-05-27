package fun.xiaorang.spring.annotation.beandefinition.beanfactorypostprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/19 0:32
 */
//@Component
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyBeanFactoryPostProcessor.class);

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		LOGGER.info("MyBeanFactoryPostProcessor#postProcessBeanFactory");
	}
}
