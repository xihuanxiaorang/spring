package fun.xiaorang.spring.annotation.beandefinition;

import fun.xiaorang.spring.annotation.beandefinition.beanfactorypostprocessor.MyManualBeanDefinitionRegistryPostProcessor;
import fun.xiaorang.spring.annotation.beandefinition.beanfactorypostprocessor.MyManualBeanFactoryPostProcessor;
import fun.xiaorang.spring.annotation.beandefinition.config.MainConfig;
import fun.xiaorang.spring.annotation.beandefinition.importbean.MyImportBeanDefinitionRegistrar;
import fun.xiaorang.spring.annotation.beandefinition.importbean.MyImportSelector;
import fun.xiaorang.spring.annotation.beandefinition.pojo.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/19 0:24
 */
public class ApiTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiTest.class);

	@Test
	public void test_00() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MainConfig.class);
		// 手动添加自定义的 BeanFactory 后置处理器
		ctx.addBeanFactoryPostProcessor(new MyManualBeanFactoryPostProcessor());
		ctx.addBeanFactoryPostProcessor(new MyManualBeanDefinitionRegistryPostProcessor());
		ctx.refresh();
	}

	@Test
	public void test_01() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfig.class);
		Eagle eagle = ctx.getBean(Eagle.class);
		LOGGER.info("{}", eagle);
		Animal.Bird bird = ctx.getBean(Animal.Bird.class);
		LOGGER.info("{}", bird);
		Animal.Tiger tiger = ctx.getBean(Animal.Tiger.class);
		LOGGER.info("{}", tiger);
	}

	@Test
	public void test_02() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfig.class);
		User user = ctx.getBean(User.class);
		LOGGER.info("{}", user);
		MyImportSelector myImportSelector = ctx.getBean(MyImportSelector.class);
		LOGGER.info("{}", myImportSelector);
	}

	@Test
	public void test_03() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfig.class);
		Person person = ctx.getBean(Person.class);
		LOGGER.info("{}", person);
		MyImportBeanDefinitionRegistrar myImportBeanDefinitionRegistrar = ctx.getBean(MyImportBeanDefinitionRegistrar.class);
		LOGGER.info("{}", myImportBeanDefinitionRegistrar);
	}

	@Test
	public void test_04() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfig.class);
		Pet pet = ctx.getBean(Pet.class);
		LOGGER.info("{}", pet);
	}
}
