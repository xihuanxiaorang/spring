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
 * @date 2023/6/2 18:02
 */
public class MyAfterAdvice implements MethodInterceptor {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyAfterAdvice.class);

	@Nullable
	@Override
	public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
		try {
			return invocation.proceed();
		} finally {
			LOGGER.info("后置通知logEnd==>目标方法：{}，参数:{}", invocation.getMethod().getName(), invocation.getArguments());
		}
	}
}
