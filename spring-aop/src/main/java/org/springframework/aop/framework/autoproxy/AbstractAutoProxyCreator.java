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

package org.springframework.aop.framework.autoproxy;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.core.SmartClassLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that wraps each eligible bean with an AOP proxy, delegating to specified interceptors
 * before invoking the bean itself.
 *
 * <p>This class distinguishes between "common" interceptors: shared for all proxies it
 * creates, and "specific" interceptors: unique per bean instance. There need not be any
 * common interceptors. If there are, they are set using the interceptorNames property.
 * As with {@link org.springframework.aop.framework.ProxyFactoryBean}, interceptors names
 * in the current factory are used rather than bean references to allow correct handling
 * of prototype advisors and interceptors: for example, to support stateful mixins.
 * Any advice type is supported for {@link #setInterceptorNames "interceptorNames"} entries.
 *
 * <p>Such auto-proxying is particularly useful if there's a large number of beans that
 * need to be wrapped with similar proxies, i.e. delegating to the same interceptors.
 * Instead of x repetitive proxy definitions for x target beans, you can register
 * one single such post processor with the bean factory to achieve the same effect.
 *
 * <p>Subclasses can apply any strategy to decide if a bean is to be proxied, e.g. by type,
 * by name, by definition details, etc. They can also return additional interceptors that
 * should just be applied to the specific bean instance. A simple concrete implementation is
 * {@link BeanNameAutoProxyCreator}, identifying the beans to be proxied via given names.
 *
 * <p>Any number of {@link TargetSourceCreator} implementations can be used to create
 * a custom target source: for example, to pool prototype objects. Auto-proxying will
 * occur even if there is no advice, as long as a TargetSourceCreator specifies a custom
 * {@link org.springframework.aop.TargetSource}. If there are no TargetSourceCreators set,
 * or if none matches, a {@link org.springframework.aop.target.SingletonTargetSource}
 * will be used by default to wrap the target bean instance.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Sam Brannen
 * @see #setInterceptorNames
 * @see #getAdvicesAndAdvisorsForBean
 * @see BeanNameAutoProxyCreator
 * @see DefaultAdvisorAutoProxyCreator
 * @since 13.10.2003
 */
@SuppressWarnings("serial")
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
		implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

	/**
	 * Convenience constant for subclasses: Return value for "do not proxy".
	 *
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	@Nullable
	protected static final Object[] DO_NOT_PROXY = null;

	/**
	 * Convenience constant for subclasses: Return value for
	 * "proxy without additional interceptors, just the common ones".
	 *
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];


	/**
	 * Logger available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());
	private final Set<String> targetSourcedBeans = Collections.newSetFromMap(new ConcurrentHashMap<>(16));
	private final Map<Object, Object> earlyProxyReferences = new ConcurrentHashMap<>(16);
	private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<>(16);
	private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256);
	/**
	 * Default is global AdvisorAdapterRegistry.
	 */
	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();
	/**
	 * Indicates whether the proxy should be frozen. Overridden from super
	 * to prevent the configuration from becoming frozen too early.
	 */
	private boolean freezeProxy = false;
	/**
	 * Default is no common interceptors.
	 */
	private String[] interceptorNames = new String[0];
	private boolean applyCommonInterceptorsFirst = true;
	@Nullable
	private TargetSourceCreator[] customTargetSourceCreators;
	@Nullable
	private BeanFactory beanFactory;

	@Override
	public boolean isFrozen() {
		return this.freezeProxy;
	}

	/**
	 * Set whether the proxy should be frozen, preventing advice
	 * from being added to it once it is created.
	 * <p>Overridden from the superclass to prevent the proxy configuration
	 * from being frozen before the proxy is created.
	 */
	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	/**
	 * Specify the {@link AdvisorAdapterRegistry} to use.
	 * <p>Default is the global {@link AdvisorAdapterRegistry}.
	 *
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	/**
	 * Set custom {@code TargetSourceCreators} to be applied in this order.
	 * If the list is empty, or they all return null, a {@link SingletonTargetSource}
	 * will be created for each bean.
	 * <p>Note that TargetSourceCreators will kick in even for target beans
	 * where no advices or advisors have been found. If a {@code TargetSourceCreator}
	 * returns a {@link TargetSource} for a specific bean, that bean will be proxied
	 * in any case.
	 * <p>{@code TargetSourceCreators} can only be invoked if this post processor is used
	 * in a {@link BeanFactory} and its {@link BeanFactoryAware} callback is triggered.
	 *
	 * @param targetSourceCreators the list of {@code TargetSourceCreators}.
	 *                             Ordering is significant: The {@code TargetSource} returned from the first matching
	 *                             {@code TargetSourceCreator} (that is, the first that returns non-null) will be used.
	 */
	public void setCustomTargetSourceCreators(TargetSourceCreator... targetSourceCreators) {
		this.customTargetSourceCreators = targetSourceCreators;
	}

	/**
	 * Set the common interceptors. These must be bean names in the current factory.
	 * They can be of any advice or advisor type Spring supports.
	 * <p>If this property isn't set, there will be zero common interceptors.
	 * This is perfectly valid, if "specific" interceptors such as matching
	 * Advisors are all we want.
	 */
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * Set whether the common interceptors should be applied before bean-specific ones.
	 * Default is "true"; else, bean-specific interceptors will get applied first.
	 */
	public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
		this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
	}

	/**
	 * Return the owning {@link BeanFactory}.
	 * May be {@code null}, as this post-processor doesn't need to belong to a bean factory.
	 */
	@Nullable
	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	@Nullable
	public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
		if (this.proxyTypes.isEmpty()) {
			return null;
		}
		Object cacheKey = getCacheKey(beanClass, beanName);
		return this.proxyTypes.get(cacheKey);
	}

	@Override
	@Nullable
	public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) {
		return null;
	}

	@Override
	public Object getEarlyBeanReference(Object bean, String beanName) {
		Object cacheKey = getCacheKey(bean.getClass(), beanName);
		this.earlyProxyReferences.put(cacheKey, bean);
		return wrapIfNecessary(bean, beanName, cacheKey);
	}

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
		// 构建缓存键名 KEY = 组件名称，如果当前组件类型为 FactoryBean 的话，则 KEY = & + 组件名称
		Object cacheKey = getCacheKey(beanClass, beanName);

		// 判断是否满足如下条件：组件名称不为空 或者 targetSourcedBeans 缓存中不存在当前组件名称（一般情况下不会包含，只有在配置了自定义 TargetSource 时才会往该缓存中添加组件，如本方法下面代码所示）
		if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
			// 判断 advisedBeans 缓存中是否存在当前组件，如果存在的话，则说明当前组件已经分析过，无需再次分析，直接返回 NULL 即可！
			if (this.advisedBeans.containsKey(cacheKey)) {
				return null;
			}
			/*
				isInfrastructureClass()：判断当前组件是否为基础组件，由子类 AnnotationAwareAspectJAutoProxyCreator 进行重写，
					判断当前组件是否为 Advice、Pointcut、Advisor、AopInfrastructureBean 类型或者切面类（类上是否标注 @Aspect 注解）
				shouldSkip()：判断当前组件是否应该跳过？由子类 AspectJAwareAdvisorAutoProxyCreator 进行重写，
					在该过程中存在一个非常重要的逻辑！！！那就是会从容器中获取所有候选的增强器 Advisor；判断当前组件是否为候选增强器中的一员
				如果满足以上任意一个条件的话，则将当前组件添加到 advisedBeans 缓存中并标记当前组件不需要进行代理操作，然后直接返回 NULL 即可！
			 */
			if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
				this.advisedBeans.put(cacheKey, Boolean.FALSE);
				return null;
			}
		}

		// Create proxy here if we have a custom TargetSource.
		// Suppresses unnecessary default instantiation of the target bean:
		// The TargetSource will handle target instances in a custom fashion.
		/*
			在创建代理对象时默认的 TargetSource 类型为 SingletonTargetSource，如果明确为当前组件指定了自定义的 TargetSource 的话，说明存在自己的代理逻辑实现，则就会直接在此处提前创建出代理对象并返回！
			当然，一般情况下并不会走该逻辑，可以算作是一个扩展点！
		 */
		TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
		if (targetSource != null) {
			if (StringUtils.hasLength(beanName)) {
				this.targetSourcedBeans.add(beanName);
			}
			// 从 IoC 容器中获取所有增强器 Advisor 并筛选出与当前组件匹配的增强器
			Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
			// 创建当前组件的代理对象
			Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		return null;
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
		return pvs;  // skip postProcessPropertyValues
	}

	/**
	 * Create a proxy with the configured interceptors if the bean is
	 * identified as one to proxy by the subclass.
	 *
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	@Override
	public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
		if (bean != null) {
			// 构建缓存键名 KEY = 组件名称，如果当前组件类型为 FactoryBean 的话，则 KEY = & + 组件名称
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			if (this.earlyProxyReferences.remove(cacheKey) != bean) {
				/*
					如果当前组件不存在于早期代理引用缓存中，则说明当前组件还没有执行过 AOP 代理操作，则为其创建代理对象
					那么什么情况下当前组件会存在于早期代理缓存呢？可以查看本类中的 getEarlyBeanReference() 方法，当当前组件与其他组件出现循环依赖并且当前组件又需要做增强处理时，
						则将创建当前组件的代理对象的时机提前到依赖注入（属性填充）阶段，最后会将当前组件保存到早期代理引用缓存中，防止在此处进行重复代理操作！
				 */
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}


	/**
	 * Build a cache key for the given bean class and bean name.
	 * <p>Note: As of 4.2.3, this implementation does not return a concatenated
	 * class/name String anymore but rather the most efficient cache key possible:
	 * a plain bean name, prepended with {@link BeanFactory#FACTORY_BEAN_PREFIX}
	 * in case of a {@code FactoryBean}; or if no bean name specified, then the
	 * given bean {@code Class} as-is.
	 *
	 * @param beanClass the bean class
	 * @param beanName  the bean name
	 * @return the cache key for the given class and name
	 */
	protected Object getCacheKey(Class<?> beanClass, @Nullable String beanName) {
		if (StringUtils.hasLength(beanName)) {
			return (FactoryBean.class.isAssignableFrom(beanClass) ?
					BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
		} else {
			return beanClass;
		}
	}

	/**
	 * Wrap the given bean if necessary, i.e. if it is eligible for being proxied.
	 *
	 * @param bean     the raw bean instance
	 * @param beanName the name of the bean
	 * @param cacheKey the cache key for metadata access
	 * @return a proxy wrapping the bean, or the raw bean instance as-is
	 */
	protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		/*
			如果当前组件存在于 targetSourcedBeans 缓存中，则说明为当前组件明确指定了自定义的 TargetSource，在当前组件实例化前的 postProcessBeforeInstantiation() 方法中已经为其创建代理对象并保存到缓存中
			此处直接返回即可！
		 */
		if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
			return bean;
		}
		// 如果 advisedBeans 缓存中存在当前组件并且标记当前组件不需要进行代理操作，则直接返回即可！
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}
		// 如果当前组件是基础组件或者不需要进行代理操作，则直接返回即可！详细分析可以查看 postProcessBeforeInstantiation() 方法，其中的逻辑大体相同
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

		// Create proxy if we have advice.
		/*
			从 IoC 容器中获取所有的增强器 Advisor （因为已经执行过 postProcessBeforeInstantiation() 方法，所以此处会直接从 advisorCache 缓存中取）并筛选出与当前组件匹配的增强器，
			如果存在与当前组件匹配的增强器的话，则为当前组件创建代理对象！
		 */
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		// 存在增强逻辑时才会进行代理
		if (specificInterceptors != DO_NOT_PROXY) {
			// 将当前组件添加到 advisedBeans 缓存中并标记当前组件需要进行代理操作
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			// 创建代理对象，目标源 TargetSource 为 SingletonTargetSource 类型
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}
		// 将当前组件添加到 advisedBeans 缓存中并标记当前组件不需要进行代理操作
		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}

	/**
	 * Return whether the given bean class represents an infrastructure class
	 * that should never be proxied.
	 * <p>The default implementation considers Advices, Advisors and
	 * AopInfrastructureBeans as infrastructure classes.
	 *
	 * @param beanClass the class of the bean
	 * @return whether the bean represents an infrastructure class
	 * @see org.aopalliance.aop.Advice
	 * @see org.springframework.aop.Advisor
	 * @see org.springframework.aop.framework.AopInfrastructureBean
	 * @see #shouldSkip
	 */
	protected boolean isInfrastructureClass(Class<?> beanClass) {
		boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
				Pointcut.class.isAssignableFrom(beanClass) ||
				Advisor.class.isAssignableFrom(beanClass) ||
				AopInfrastructureBean.class.isAssignableFrom(beanClass);
		if (retVal && logger.isTraceEnabled()) {
			logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
		}
		return retVal;
	}

	/**
	 * Subclasses should override this method to return {@code true} if the
	 * given bean should not be considered for auto-proxying by this post-processor.
	 * <p>Sometimes we need to be able to avoid this happening, e.g. if it will lead to
	 * a circular reference or if the existing target instance needs to be preserved.
	 * This implementation returns {@code false} unless the bean name indicates an
	 * "original instance" according to {@code AutowireCapableBeanFactory} conventions.
	 *
	 * @param beanClass the class of the bean
	 * @param beanName  the name of the bean
	 * @return whether to skip the given bean
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#ORIGINAL_INSTANCE_SUFFIX
	 */
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		return AutoProxyUtils.isOriginalInstance(beanName, beanClass);
	}

	/**
	 * Create a target source for bean instances. Uses any TargetSourceCreators if set.
	 * Returns {@code null} if no custom TargetSource should be used.
	 * <p>This implementation uses the "customTargetSourceCreators" property.
	 * Subclasses can override this method to use a different mechanism.
	 *
	 * @param beanClass the class of the bean to create a TargetSource for
	 * @param beanName  the name of the bean
	 * @return a TargetSource for this bean
	 * @see #setCustomTargetSourceCreators
	 */
	@Nullable
	protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
		// We can't create fancy target sources for directly registered singletons.
		if (this.customTargetSourceCreators != null &&
				this.beanFactory != null && this.beanFactory.containsBean(beanName)) {
			for (TargetSourceCreator tsc : this.customTargetSourceCreators) {
				TargetSource ts = tsc.getTargetSource(beanClass, beanName);
				if (ts != null) {
					// Found a matching TargetSource.
					if (logger.isTraceEnabled()) {
						logger.trace("TargetSourceCreator [" + tsc +
								"] found custom TargetSource for bean with name '" + beanName + "'");
					}
					return ts;
				}
			}
		}

		// No custom TargetSource found.
		return null;
	}

	/**
	 * Create an AOP proxy for the given bean.
	 *
	 * @param beanClass            the class of the bean
	 * @param beanName             the name of the bean
	 * @param specificInterceptors the set of interceptors that is
	 *                             specific to this bean (may be empty, but not null)
	 * @param targetSource         the TargetSource for the proxy,
	 *                             already pre-configured to access the bean
	 * @return the AOP proxy for the bean
	 * @see #buildAdvisors
	 */
	protected Object createProxy(Class<?> beanClass, @Nullable String beanName,
								 @Nullable Object[] specificInterceptors, TargetSource targetSource) {

		if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
			AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
		}

		// 创建一个代理工厂 ProxyFactory 对象，专门用来创建动态代理的工厂，可以通过其 getProxy() 方法获取/创建代理对象！
		ProxyFactory proxyFactory = new ProxyFactory();
		// ProxyFactory 与 AnnotationAwareAspectJAutoProxyCreator 都继承自 ProxyConfig，所以将 AnnotationAwareAspectJAutoProxyCreator 中的相关属性拷贝一份到 ProxyConfig 中
		proxyFactory.copyFrom(this);

		// 如果显示指定 proxyFactory 中的 proxyTargetClass 属性设置为 true，则使用 Cglib 动态代理
		if (proxyFactory.isProxyTargetClass()) {
			// Explicit handling of JDK proxy targets and lambdas (for introduction advice scenarios)
			// 但是目标类是已经经过 JDK 动态代理生成的代理类或者是 Lambda 表达式的话
			if (Proxy.isProxyClass(beanClass) || ClassUtils.isLambdaClass(beanClass)) {
				// Must allow for introductions; can't just set interfaces to the proxy's interfaces only.
				for (Class<?> ifc : beanClass.getInterfaces()) {
					// 则将当前组件所实现的接口添加到 proxyFactory 的 interfaces 属性中
					proxyFactory.addInterface(ifc);
				}
			}
		} else {
			// No proxyTargetClass flag enforced, let's apply our default checks...
			// 判断当前组件的 BeanDefinition 中是否配置 preserveTargetClass 属性值为 true，如果为 true 的话，则使用 Cglib 动态代理，指定 proxyFactory 中的 proxyTargetClass 属性设置为 true
			if (shouldProxyTargetClass(beanClass, beanName)) {
				proxyFactory.setProxyTargetClass(true);
			} else {
				/*
					如果当前组件有符合要求的接口，则将接口添加到 proxyFactory 的 interfaces 属性中；
					如果当前组件没有符合要求的接口，那就只能基于类代理，即使用 Cglib 动态代理，指定 proxyFactory 中的 proxyTargetClass 属性设置为 true
				 */
				evaluateProxyInterfaces(beanClass, proxyFactory);
			}
		}

		// 构建增强器集合（将匹配到的增强器和普通的增强器（一般为空）进行合并）
		Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
		// 将增强器集合设置到 proxyFactory 中的 advisors 属性
		proxyFactory.addAdvisors(advisors);
		// 设置目标源 TargetSource，一般情况下为 SingletonTargetSource
		proxyFactory.setTargetSource(targetSource);
		customizeProxyFactory(proxyFactory);

		proxyFactory.setFrozen(this.freezeProxy);
		if (advisorsPreFiltered()) {
			proxyFactory.setPreFiltered(true);
		}

		// Use original ClassLoader if bean class not locally loaded in overriding class loader
		ClassLoader classLoader = getProxyClassLoader();
		if (classLoader instanceof SmartClassLoader && classLoader != beanClass.getClassLoader()) {
			classLoader = ((SmartClassLoader) classLoader).getOriginalClassLoader();
		}
		// 真正创建代理对象的地方
		return proxyFactory.getProxy(classLoader);
	}

	/**
	 * Determine whether the given bean should be proxied with its target class rather than its interfaces.
	 * <p>Checks the {@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}
	 * of the corresponding bean definition.
	 *
	 * @param beanClass the class of the bean
	 * @param beanName  the name of the bean
	 * @return whether the given bean should be proxied with its target class
	 * @see AutoProxyUtils#shouldProxyTargetClass
	 */
	protected boolean shouldProxyTargetClass(Class<?> beanClass, @Nullable String beanName) {
		return (this.beanFactory instanceof ConfigurableListableBeanFactory &&
				AutoProxyUtils.shouldProxyTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName));
	}

	/**
	 * Return whether the Advisors returned by the subclass are pre-filtered
	 * to match the bean's target class already, allowing the ClassFilter check
	 * to be skipped when building advisors chains for AOP invocations.
	 * <p>Default is {@code false}. Subclasses may override this if they
	 * will always return pre-filtered Advisors.
	 *
	 * @return whether the Advisors are pre-filtered
	 * @see #getAdvicesAndAdvisorsForBean
	 * @see org.springframework.aop.framework.Advised#setPreFiltered
	 */
	protected boolean advisorsPreFiltered() {
		return false;
	}

	/**
	 * Determine the advisors for the given bean, including the specific interceptors
	 * as well as the common interceptor, all adapted to the Advisor interface.
	 *
	 * @param beanName             the name of the bean
	 * @param specificInterceptors the set of interceptors that is
	 *                             specific to this bean (may be empty, but not null)
	 * @return the list of Advisors for the given bean
	 */
	protected Advisor[] buildAdvisors(@Nullable String beanName, @Nullable Object[] specificInterceptors) {
		// Handle prototypes correctly...
		Advisor[] commonInterceptors = resolveInterceptorNames();

		List<Object> allInterceptors = new ArrayList<>();
		if (specificInterceptors != null) {
			if (specificInterceptors.length > 0) {
				// specificInterceptors may equal PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
				allInterceptors.addAll(Arrays.asList(specificInterceptors));
			}
			if (commonInterceptors.length > 0) {
				if (this.applyCommonInterceptorsFirst) {
					allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
				} else {
					allInterceptors.addAll(Arrays.asList(commonInterceptors));
				}
			}
		}
		if (logger.isTraceEnabled()) {
			int nrOfCommonInterceptors = commonInterceptors.length;
			int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
			logger.trace("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
					" common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
		}

		Advisor[] advisors = new Advisor[allInterceptors.size()];
		for (int i = 0; i < allInterceptors.size(); i++) {
			advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
		}
		return advisors;
	}

	/**
	 * Resolves the specified interceptor names to Advisor objects.
	 *
	 * @see #setInterceptorNames
	 */
	private Advisor[] resolveInterceptorNames() {
		BeanFactory bf = this.beanFactory;
		ConfigurableBeanFactory cbf = (bf instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) bf : null);
		List<Advisor> advisors = new ArrayList<>();
		for (String beanName : this.interceptorNames) {
			if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
				Assert.state(bf != null, "BeanFactory required for resolving interceptor names");
				Object next = bf.getBean(beanName);
				advisors.add(this.advisorAdapterRegistry.wrap(next));
			}
		}
		return advisors.toArray(new Advisor[0]);
	}

	/**
	 * Subclasses may choose to implement this: for example,
	 * to change the interfaces exposed.
	 * <p>The default implementation is empty.
	 *
	 * @param proxyFactory a ProxyFactory that is already configured with
	 *                     TargetSource and interfaces and will be used to create the proxy
	 *                     immediately after this method returns
	 */
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}


	/**
	 * Return whether the given bean is to be proxied, what additional
	 * advices (e.g. AOP Alliance interceptors) and advisors to apply.
	 *
	 * @param beanClass          the class of the bean to advise
	 * @param beanName           the name of the bean
	 * @param customTargetSource the TargetSource returned by the
	 *                           {@link #getCustomTargetSource} method: may be ignored.
	 *                           Will be {@code null} if no custom target source is in use.
	 * @return an array of additional interceptors for the particular bean;
	 * or an empty array if no additional interceptors but just the common ones;
	 * or {@code null} if no proxy at all, not even with the common interceptors.
	 * See constants DO_NOT_PROXY and PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS.
	 * @throws BeansException in case of errors
	 * @see #DO_NOT_PROXY
	 * @see #PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
	 */
	@Nullable
	protected abstract Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
															 @Nullable TargetSource customTargetSource) throws BeansException;

}
