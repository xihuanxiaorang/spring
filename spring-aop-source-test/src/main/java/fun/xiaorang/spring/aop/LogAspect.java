package fun.xiaorang.spring.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; ">日志切面<p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/29 16:32
 */
@Component
@Aspect
public class LogAspect {
	private static final Logger LOGGER = LoggerFactory.getLogger(LogAspect.class);

	public LogAspect() {
		LOGGER.info("...LogAspect创建了...");
	}

	@Pointcut("execution(* fun.xiaorang.spring.aop.service.EchoService.echo(..))")
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
		LOGGER.info("前置通知logStart==>目标方法：{}，参数:{}", name, joinPoint.getArgs());
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
		LOGGER.info("返回通知logReturn==>目标方法：{}，参数:{}，返回值：{}", name, joinPoint.getArgs(), result);
	}

	/**
	 * 后置通知
	 *
	 * @param joinPoint 封装了 AOP 中切面方法的信息
	 */
	@After("pointcut()")
	public void logEnd(JoinPoint joinPoint) {
		String name = joinPoint.getSignature().getName();
		LOGGER.info("后置通知logEnd==>目标方法：{}，参数:{}", name, joinPoint.getArgs());
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
		LOGGER.info("异常通知logError==>目标方法：{}，参数:{}，异常信息: {}", name, joinPoint.getArgs(), e.getMessage());
	}

	/**
	 * 环绕通知
	 *
	 * @param proceedingJoinPoint 封装了 AOP 中切面方法的信息
	 * @return 目标方法的返回值
	 */
	@Around("pointcut()")
	public Object logAround(ProceedingJoinPoint proceedingJoinPoint) {
		String name = proceedingJoinPoint.getSignature().getName();
		Object result;
		try {
			LOGGER.info("环绕通知logAround前==>目标方法：{}，参数:{}", name, proceedingJoinPoint.getArgs());
			result = proceedingJoinPoint.proceed();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			LOGGER.info("环绕通知logAround后==>目标方法：{}，参数:{}", name, proceedingJoinPoint.getArgs());
		}
		return result;
	}
}
