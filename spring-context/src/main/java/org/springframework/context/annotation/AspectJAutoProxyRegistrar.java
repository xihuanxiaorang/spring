/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Registers an {@link org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator
 * AnnotationAwareAspectJAutoProxyCreator} against the current {@link BeanDefinitionRegistry}
 * as appropriate based on a given @{@link EnableAspectJAutoProxy} annotation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see EnableAspectJAutoProxy
 * @since 3.1
 */
class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

	/**
	 * Register, escalate, and configure the AspectJ auto proxy creator based on the value
	 * of the @{@link EnableAspectJAutoProxy#proxyTargetClass()} attribute on the importing
	 * {@code @Configuration} class.
	 */
	@Override
	public void registerBeanDefinitions(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		// 注册基于注解的自动代理创建器「AnnotationAwareAspectJAutoProxyCreator」的 BeanDefinition
		AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);

		/*
			将 @EnableAspectJAutoProxy 注解上配置的属性信息赋值给 AnnotationAwareAspectJAutoProxyCreator 后置处理器，如：
			1. proxyTargetClass：布尔类型，当该属性值为 true 时，表示将会使用 Cglib 动态代理的方式生成代理对象；
			2. exposeProxy：布尔类型，表示是否暴露当前代理对象，即将当前代理对象存储在 AopContext 上下文的 currentProxy 属性中，使用 ThreadLocal 的方式将当前暴露的代理对象与线程绑定起来，
				后续要使用的时候可以通过上下文中的 currentProxy() 方法获取出该代理对象。可以用来解决在同一个类中非事务方法A调用事务方法B会导致事务失效的情况！
		 */
		AnnotationAttributes enableAspectJAutoProxy =
				AnnotationConfigUtils.attributesFor(importingClassMetadata, EnableAspectJAutoProxy.class);
		if (enableAspectJAutoProxy != null) {
			if (enableAspectJAutoProxy.getBoolean("proxyTargetClass")) {
				AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
			}
			if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {
				AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
			}
		}
	}

}
