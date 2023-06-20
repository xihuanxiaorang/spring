/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.aop.SpringProxy;
import org.springframework.core.NativeDetector;
import org.springframework.util.ClassUtils;

import java.io.Serializable;
import java.lang.reflect.Proxy;

/**
 * Default {@link AopProxyFactory} implementation, creating either a CGLIB proxy
 * or a JDK dynamic proxy.
 *
 * <p>Creates a CGLIB proxy if one the following is true for a given
 * {@link AdvisedSupport} instance:
 * <ul>
 * <li>the {@code optimize} flag is set
 * <li>the {@code proxyTargetClass} flag is set
 * <li>no proxy interfaces have been specified
 * </ul>
 *
 * <p>In general, specify {@code proxyTargetClass} to enforce a CGLIB proxy,
 * or specify one or more interfaces to use a JDK dynamic proxy.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 * @since 12.03.2004
 */
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

	private static final long serialVersionUID = 7930414337282325166L;


	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		/*
			当前 config 参数为 ProxyFactory 实例对象，根据以下配置条件选择创建 ObjenesisCglibAopProxy 或 JdkDynamicAopProxy 的实例对象返回
			1. 判断是否是在 graalvm 虚拟机环境上运行，如果是的话，则只能选择使用 JDK 动态代理
			2. 是否开启优化策略
			3. 判断是否使用 Cglib 动态代理，可以通过以下两种方式进行设置：
				a. 手动指定 ProxyFactory 中的 proxyTargetClass 属性设置为 true
				b. 在开启 Aop 功能时设置 @EnableAspectJAutoProxy 注解中的 proxyTargetClass 属性为 true
			4. 判断当前目标类是否没有实现任何接口或者实现了一个接口但是该接口是 SpringProxy 类型，如果有实现接口并且只实现了一个接口时也不是 SpringProxy 类型的话，则使用 JDK 动态代理
			因为这几个条件是用或（|）连接的，所以前面的条件满足就不再判断后面的条件，即排在前面的条件具有更高的优先级
		 */
		if (!NativeDetector.inNativeImage() &&
				(config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config))) {
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			// 若目标类是接口或是已经经过 JDK 动态代理生成的代理类或是 Lambda 表达式的话，则只能使用 JDK 动态代理
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass) || ClassUtils.isLambdaClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			// 表明目标类并不是接口或者代理类，则使用 Cglib 动态代理
			return new ObjenesisCglibAopProxy(config);
		} else {
			// 使用 JDK 动态代理（即(没有开启优化策略，也没有设置使用 Cglib 动态代理，同时目标类有实现接口并且只实现了一个接口时也不是 SpringProxy 类型)或者当前是在 graalvm 虚拟机环境上运行 ）
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		// 当前目标类没有实现任何接口或者实现了一个接口但是该接口是 SpringProxy 类型时，返回 true
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
