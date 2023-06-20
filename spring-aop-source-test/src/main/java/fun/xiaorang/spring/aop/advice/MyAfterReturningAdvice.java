package fun.xiaorang.spring.aop.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.AfterReturningAdvice;

import java.lang.reflect.Method;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/6/2 18:03
 */
public class MyAfterReturningAdvice implements AfterReturningAdvice {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyAfterReturningAdvice.class);

	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		LOGGER.info("返回通知logReturn==>目标方法：{}，参数:{}，返回值：{}", method.getName(), args, returnValue);
	}
}
