package fun.xiaorang.spring.aop.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.MethodBeforeAdvice;

import java.lang.reflect.Method;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/6/2 17:58
 */
public class MyMethodBeforeAdvice implements MethodBeforeAdvice {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyMethodBeforeAdvice.class);

	@Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		LOGGER.info("前置通知logStart==>目标方法：{}，参数:{}", method.getName(), args);
	}
}
