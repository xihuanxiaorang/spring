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

package org.springframework.aop;

import org.springframework.lang.Nullable;

/**
 * A {@code TargetSource} is used to obtain the current "target" of
 * an AOP invocation, which will be invoked via reflection if no around
 * advice chooses to end the interceptor chain itself.
 *
 * <p>If a {@code TargetSource} is "static", it will always return
 * the same target, allowing optimizations in the AOP framework. Dynamic
 * target sources can support pooling, hot swapping, etc.
 *
 * <p>Application developers don't usually need to work with
 * {@code TargetSources} directly: this is an AOP framework interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface TargetSource extends TargetClassAware {

	/**
	 * 返回目标对象类型
	 * Return the type of targets returned by this {@link TargetSource}.
	 * <p>Can return {@code null}, although certain usages of a {@code TargetSource}
	 * might just work with a predetermined target class.
	 *
	 * @return the type of targets returned by this {@link TargetSource}
	 */
	@Override
	@Nullable
	Class<?> getTargetClass();

	/**
	 * 用于确定调用 getTarget() 方法时返回的目标对象是不是同一个？如果为true的话，表示返回的同一个目标对象，此时不再需要调用 releaseTarget() 方法，因为 Aop 框架可以缓存目标对象
	 * 在其实现类 SingletonTargetSource 中该方法返回 true，表示调用 getTarget() 方法时返回的目标对象是同一个单例对象，
	 * 而在其实现类 PrototypeTargetSource 中该方法返回 false，表示调用 getTarget() 方法时返回的目标对象是多例对象
	 * <p>
	 * Will all calls to {@link #getTarget()} return the same object?
	 * <p>In that case, there will be no need to invoke {@link #releaseTarget(Object)},
	 * and the AOP framework can cache the return value of {@link #getTarget()}.
	 *
	 * @return {@code true} if the target is immutable
	 * @see #getTarget
	 */
	boolean isStatic();

	/**
	 * 获取目标对象，在每次方法调用（MethodInvocation）执行（proceed()）之前立即获取
	 * Return a target instance. Invoked immediately before the
	 * AOP framework calls the "target" of an AOP method invocation.
	 *
	 * @return the target object which contains the joinpoint,
	 * or {@code null} if there is no actual target instance
	 * @throws Exception if the target object can't be resolved
	 */
	@Nullable
	Object getTarget() throws Exception;

	/**
	 * 释放从 getTarget() 方法获得的给定目标对象(如果有的话)
	 * Release the given target object obtained from the
	 * {@link #getTarget()} method, if any.
	 *
	 * @param target object obtained from a call to {@link #getTarget()}
	 * @throws Exception if the object can't be released
	 */
	void releaseTarget(Object target) throws Exception;

}
