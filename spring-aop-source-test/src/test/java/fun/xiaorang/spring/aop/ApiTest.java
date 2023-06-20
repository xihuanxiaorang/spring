package fun.xiaorang.spring.aop;

import fun.xiaorang.spring.aop.advice.*;
import fun.xiaorang.spring.aop.config.MainConfig;
import fun.xiaorang.spring.aop.service.EchoService;
import fun.xiaorang.spring.aop.service.EchoServiceImpl;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/29 16:34
 */
class ApiTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiTest.class);

	@Test
	public void test_00() {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(MainConfig.class);
		EchoService echoService = applicationContext.getBean("echoServiceImpl", EchoService.class);
		LOGGER.info("======================华丽的分割线=========================");
		echoService.echo("小让");
		LOGGER.info("======================华丽的分割线=========================");
	}


	@Test
	public void test_01() {
		EchoService echoService = new EchoServiceImpl();
		// 指定目标对象和设置目标对象所实现的所有接口
		ProxyFactory proxyFactory = new ProxyFactory(echoService);
		// 使用 Cglib 动态代理的方式生成代理对象
		proxyFactory.setProxyTargetClass(true);
		// 环绕通知
		proxyFactory.addAdvice(new MyAroundAdvice());
		// 前置通知
		proxyFactory.addAdvice(new MyMethodBeforeAdvice());
		// 后置通知
		proxyFactory.addAdvice(new MyAfterAdvice());
		// 返回通知
		proxyFactory.addAdvice(new MyAfterReturningAdvice());
		// 异常通知
		proxyFactory.addAdvice(new MyAfterThrowingAdvice());
		// 创建代理对象
		EchoService proxy = (EchoService) proxyFactory.getProxy();
		LOGGER.info("======================华丽的分割线=========================");
		proxy.echo("小让");
		LOGGER.info("======================华丽的分割线=========================");
	}
}
