package fun.xiaorang.spring.annotation.beandefinition.beanfactorypostprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/19 0:31
 */
public class MyManualBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyManualBeanDefinitionRegistryPostProcessor.class);

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		LOGGER.info("MyManualBeanDefinitionRegistryPostProcessor#postProcessBeanFactory");
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		LOGGER.info("MyManualBeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry");
	}
}
