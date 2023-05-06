package fun.xiaorang.spring.beandefinition;

import fun.xiaorang.spring.beandefinition.service.UserService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/4/28 3:08
 */
class ApiTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiTest.class);

	@Test
	public void test_00() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
		User user = ctx.getBean(User.class);
		LOGGER.info(String.valueOf(user));
		UserService userService = ctx.getBean(UserService.class);
		LOGGER.info(userService.getUserInfo("xiaorang"));
	}
}