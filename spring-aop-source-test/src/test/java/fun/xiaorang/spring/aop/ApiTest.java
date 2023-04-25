package fun.xiaorang.spring.aop;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/4/26 2:29
 */
class ApiTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiTest.class);

	@Test
	public void test() {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(MainConfig.class);
		HelloService helloService = applicationContext.getBean(HelloService.class);
		LOGGER.info("======================华丽的分割线=========================");
		helloService.sayHello("小让");
		LOGGER.info("======================华丽的分割线=========================");
	}
}