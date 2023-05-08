package fun.xiaorang.spring.beanlifecycle;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/6 17:29
 */
public class ApiTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiTest.class);

	@Test
	public void test_00() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
		BeanLifecycle bean = ctx.getBean(BeanLifecycle.class);
		LOGGER.info("{} 使用中...", bean);
		ctx.close();
	}
}
