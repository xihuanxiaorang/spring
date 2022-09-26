package top.xiaorang.beandefinition.service.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import top.xiaorang.beandefinition.service.People;
import top.xiaorang.beandefinition.service.UserService;

/**
 * @author liulei
 * @description
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/9/24 15:22
 */
public class SpringBeanDefinitionTests {
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringBeanDefinitionTests.class);

	public static void main(String[] args) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		UserService userService = applicationContext.getBean(UserService.class);
		LOGGER.info(userService.queryUserInfo());
		People people = applicationContext.getBean(People.class);
		LOGGER.info(people.toString());
	}
}
