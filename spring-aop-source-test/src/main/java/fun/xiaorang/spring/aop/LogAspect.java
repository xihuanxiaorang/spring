package fun.xiaorang.spring.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/4/26 2:27
 */
@Component
@Aspect
public class LogAspect {
	private static final Logger LOGGER = LoggerFactory.getLogger(LogAspect.class);

	public LogAspect() {
		LOGGER.info("...LogAspect创建了...");
	}

	@Pointcut("execution(* fun.xiaorang.spring.aop.HelloService.sayHello(..))")
	public void pointcut() {
	}

	/**
	 * 前置通知，增强方法/增强器
	 *
	 * @param joinPoint 封装了 AOP 中切面方法的信息
	 */
	@Before("pointcut()")
	public void logStart(JoinPoint joinPoint) {
		String name = joinPoint.getSignature().getName();
		LOGGER.info("前置通知logStart==>{}===【args:{}}】", name, Arrays.asList(joinPoint.getArgs()));
	}

	/**
	 * 返回通知
	 *
	 * @param joinPoint 封装了 AOP 中切面方法的信息
	 * @param result    目标方法的返回值
	 */
	@AfterReturning(value = "pointcut()", returning = "result")
	public void logReturn(JoinPoint joinPoint, Object result) {
		String name = joinPoint.getSignature().getName();
		LOGGER.info("返回通知logReturn==>{}===【args:{}}】【result：{}】", name, Arrays.asList(joinPoint.getArgs()), result);
	}

	/**
	 * 后置通知
	 *
	 * @param joinPoint 封装了 AOP 中切面方法的信息
	 */
	@After("pointcut()")
	public void logEnd(JoinPoint joinPoint) {
		String name = joinPoint.getSignature().getName();
		LOGGER.info("后置通知logEnd==>{}===【args:{}】", name, Arrays.asList(joinPoint.getArgs()));
	}

	/**
	 * 异常通知
	 *
	 * @param joinPoint 封装了 AOP 中切面方法的信息
	 * @param e         异常
	 */
	@AfterThrowing(value = "pointcut()", throwing = "e")
	public void logError(JoinPoint joinPoint, Exception e) {
		String name = joinPoint.getSignature().getName();
		LOGGER.info("异常通知logError==>{}===【args:{}】【exception: {}】", name, Arrays.asList(joinPoint.getArgs()), e.getMessage());
	}
}
