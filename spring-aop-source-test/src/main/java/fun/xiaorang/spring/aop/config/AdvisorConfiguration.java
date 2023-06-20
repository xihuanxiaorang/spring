package fun.xiaorang.spring.aop.config;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StopWatch;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/6/19 17:49
 */
@Configuration
public class AdvisorConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(AdvisorConfiguration.class);

	@Bean
	public PointcutAdvisor timingAdvisor() {
		return new DefaultPointcutAdvisor(Pointcut.TRUE, new MethodInterceptor() {
			@Nullable
			@Override
			public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
				String methodName = invocation.getMethod().getName();
				Object[] args = invocation.getArguments();
				StopWatch stopWatch = new StopWatch();
				stopWatch.start("methodCallTiming");
				try {
					return invocation.proceed();
				} finally {
					stopWatch.stop();
					LOGGER.debug("目标方法：{}，参数：{}，执行时间：{} ms", methodName, args, stopWatch.getLastTaskTimeMillis());
				}
			}
		});
	}
}
