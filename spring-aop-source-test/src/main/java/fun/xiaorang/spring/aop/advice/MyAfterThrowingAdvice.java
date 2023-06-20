package fun.xiaorang.spring.aop.advice;


import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/6/2 18:04
 */
public class MyAfterThrowingAdvice implements MethodInterceptor {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyAfterThrowingAdvice.class);

	@Nullable
	@Override
	public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
		try {
			return invocation.proceed();
		} catch (Throwable ex) {
			LOGGER.info("异常通知logError==>目标方法：{}，参数:{}，异常信息: {}", invocation.getMethod().getName(), invocation.getArguments(), ex.getMessage());
			throw ex;
		}
	}
}
