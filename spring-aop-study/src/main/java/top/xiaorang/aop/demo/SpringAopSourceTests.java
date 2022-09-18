package top.xiaorang.aop.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.xiaorang.aop.config.MainConfig;
import top.xiaorang.aop.service.HelloService;

/**
 * @author liulei
 * @description 测试类
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/9/16 4:27
 */
public class SpringAopSourceTests {
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringAopSourceTests.class);

	public static void main(String[] args) {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(MainConfig.class);
		HelloService helloService = applicationContext.getBean(HelloService.class);
		LOGGER.info("======================华丽的分割线=========================");
		helloService.sayHello("小让");
		LOGGER.info("======================华丽的分割线=========================");
	}
}
