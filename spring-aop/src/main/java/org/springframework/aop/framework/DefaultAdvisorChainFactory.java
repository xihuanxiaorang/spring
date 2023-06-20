/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.*;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple but definitive way of working out an advice chain for a Method,
 * given an {@link Advised} object. Always rebuilds each advice chain;
 * caching can be provided by subclasses.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0.3
 */
@SuppressWarnings("serial")
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {

	/**
	 * Determine whether the Advisors contain matching introductions.
	 */
	private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (ia.getClassFilter().matches(actualClass)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
			Advised config, Method method, @Nullable Class<?> targetClass) {

		// This is somewhat tricky... We have to process introductions first,
		// but we need to preserve order in the ultimate list.
		/*
			在将增强器（Advisor）中的通知（Advice）转换为方法拦截器（MethodInterceptor）时，可能并不适配，即某个通知并没有实现方法拦截器接口，此时就需要一个适配器（Adapter）进行适配将通知转换为方法拦截器
			而在增强器适配器注册中心（AdvisorAdapterRegistry）中就维护着这些适配器的，默认实现为 DefaultAdvisorAdapterRegistry，在该类进行实例化的时候就已经注册了以下三种通知的适配器：
				1. @Before -> MethodBeforeAdvice -> MethodBeforeAdviceAdapter -> MethodBeforeAdviceInterceptor
				2. @AfterReturning -> AfterReturningAdvice -> AfterReturningAdviceAdapter -> AfterReturningAdviceInterceptor
				3. ThrowsAdvice -> ThrowsAdviceAdapter -> ThrowsAdviceInterceptor，这一种好像没用到
		 */
		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
		// 获取添加到当前 ProxyFactory 实例对象中的所有增强器
		Advisor[] advisors = config.getAdvisors();
		// 用于存储方法拦截器的集合
		List<Object> interceptorList = new ArrayList<>(advisors.length);
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
		Boolean hasIntroductions = null;

		// 遍历所有的增强器，筛选出与当前目标类与方法所匹配的增强器，并将这些增强器中的通知转换为方法拦截器后添加到 interceptorList 集合中进行保存
		for (Advisor advisor : advisors) {
			if (advisor instanceof PointcutAdvisor) {
				// Add it conditionally.
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
				// 先在类级别上判断当前目标类是否与当前增强器匹配，即调用 ClassFilter 中的 matches() 方法进行判断
				if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
					boolean match;
					// 然后才在方法级别上判断当前目标方法是否与当前增强器匹配，即调用 MethodMatcher 中的 matches() 方法进行判断
					if (mm instanceof IntroductionAwareMethodMatcher) {
						if (hasIntroductions == null) {
							hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
						}
						match = ((IntroductionAwareMethodMatcher) mm).matches(method, actualClass, hasIntroductions);
					} else {
						match = mm.matches(method, actualClass);
					}
					// 匹配成功的处理
					if (match) {
						// 将当前增强器中的通知转换为方法拦截器，如果是 MethodBeforeAdvice 和 AfterReturningAdvice 这两种通知的话则需要用到对应的适配器才能为方法拦截器
						MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
						if (mm.isRuntime()) {
							// Creating a new object instance in the getInterceptors() method
							// isn't a problem as we normally cache created chains.
							/*
								如果是运行时匹配，则还需要将方法拦截器连同方法匹配器（MethodMatcher）再包装一层为 InterceptorAndDynamicMethodMatcher，
									在拦截器链执行到当前方法拦截器时，会先通过方法匹配器（MethodMatcher）中有三个参数的 matches() 方法进一步对传入的实参进行匹配，
									如果匹配的话，才会执行方法拦截器中的逻辑，否则的话，跳过该方法拦截器
							 */
							for (MethodInterceptor interceptor : interceptors) {
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
							}
						} else {
							// 添加普通的方法拦截器
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
				}
			}
			// 引介增强的处理
			else if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
					Interceptor[] interceptors = registry.getInterceptors(advisor);
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			}
			// 其它增强的处理
			else {
				Interceptor[] interceptors = registry.getInterceptors(advisor);
				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}
		// 返回拦截器链
		return interceptorList;
	}

}
