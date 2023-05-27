package fun.xiaorang.spring.annotation.beandefinition.beanfactorypostprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/19 0:31
 */
//@Component
public class MyBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyBeanDefinitionRegistryPostProcessor.class);

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		LOGGER.info("MyBeanDefinitionRegistryPostProcessor#postProcessBeanFactory");
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		LOGGER.info("MyBeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry");
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(MyBeanDefinitionRegistryPostProcessor2.class);
		registry.registerBeanDefinition(MyBeanDefinitionRegistryPostProcessor2.class.getName(), beanDefinition);
		LOGGER.info("向容器中注册 MyBeanDefinitionRegistryPostProcessor2 后置处理器");
	}
}
