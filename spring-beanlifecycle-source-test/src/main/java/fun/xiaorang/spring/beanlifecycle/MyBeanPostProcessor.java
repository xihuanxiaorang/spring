package fun.xiaorang.spring.beanlifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/6 17:23
 */
public class MyBeanPostProcessor implements InstantiationAwareBeanPostProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyBeanPostProcessor.class);

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		if (beanClass == BeanLifecycle.class) {
			LOGGER.info("postProcessBeforeInstantiation");
		}
		return null;
	}

	@Override
	public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		if (bean instanceof BeanLifecycle) {
			LOGGER.info("postProcessAfterInstantiation");
		}
		return true;
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
		if (bean instanceof BeanLifecycle) {
			LOGGER.info("postProcessProperties");
		}
		return null;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof BeanLifecycle) {
			LOGGER.info("postProcessBeforeInitialization");
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof BeanLifecycle) {
			LOGGER.info("postProcessAfterInitialization");
		}
		return bean;
	}
}
