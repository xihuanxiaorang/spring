/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper for retrieving @AspectJ beans from a BeanFactory and building
 * Spring Advisors based on them, for use with auto-proxying.
 *
 * @author Juergen Hoeller
 * @see AnnotationAwareAspectJAutoProxyCreator
 * @since 2.0.2
 */
public class BeanFactoryAspectJAdvisorsBuilder {

	private final ListableBeanFactory beanFactory;

	private final AspectJAdvisorFactory advisorFactory;
	private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();
	private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();
	@Nullable
	private volatile List<String> aspectBeanNames;


	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 *
	 * @param beanFactory the ListableBeanFactory to scan
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
		this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
	}

	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 *
	 * @param beanFactory    the ListableBeanFactory to scan
	 * @param advisorFactory the AspectJAdvisorFactory to build each Advisor with
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
		this.beanFactory = beanFactory;
		this.advisorFactory = advisorFactory;
	}


	/**
	 * Look for AspectJ-annotated aspect beans in the current bean factory,
	 * and return to a list of Spring AOP Advisors representing them.
	 * <p>Creates a Spring Advisor for each AspectJ advice method.
	 *
	 * @return the list of {@link org.springframework.aop.Advisor} beans
	 * @see #isEligibleBean
	 */
	public List<Advisor> buildAspectJAdvisors() {
		// 从 aspectBeanNames 缓存中获取所有标注了 @Aspect 注解的切面类名称
		List<String> aspectNames = this.aspectBeanNames;
		// DCL：双重检查锁
		if (aspectNames == null) {
			synchronized (this) {
				aspectNames = this.aspectBeanNames;
				if (aspectNames == null) {
					// 用于保存通过解析切面类中通知方法而构建成的增强器集合
					List<Advisor> advisors = new ArrayList<>();
					aspectNames = new ArrayList<>();
					// 从容器中获取所有组件的名称
					String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
							this.beanFactory, Object.class, true, false);
					// 遍历从容器中获取到的所有组件名称
					for (String beanName : beanNames) {
						if (!isEligibleBean(beanName)) {
							continue;
						}
						// We must be careful not to instantiate beans eagerly as in this case they
						// would be cached by the Spring container but would not have been weaved.
						// 获取当前组件所对应的 Class 对象
						Class<?> beanType = this.beanFactory.getType(beanName, false);
						if (beanType == null) {
							continue;
						}
						// 根据 Class 对象判断当前组件是否为一个切面类，即类上是否标注 @Aspect 注解
						if (this.advisorFactory.isAspect(beanType)) {
							// 将当前切面组件的名称保存到 aspectNames 缓存中
							aspectNames.add(beanName);
							// 将当前切面组件的名称 beanName 和 Class 对象封装成一个切面元数据对象 AspectMetadata
							AspectMetadata amd = new AspectMetadata(beanType, beanName);
							if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
								MetadataAwareAspectInstanceFactory factory =
										new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
								/*
									利用增强器工厂将当前切面组件中标注 @Around、@Before、@After、@AfterReturning、@AfterThrowing 注解的通知方法构建成一个个的增强器 Advisor，
									该增强器 Advisor 为 InstantiationModelAwarePointcutAdvisorImpl 类型的实例对象，
									如果当前切面组件是单例的，则将当前切面组件名称作为 KEY，由通知方法构建成的增强器集合作为 VALUE 保存到 advisorsCache 缓存中；
									如果当前切面组件不是单例的，则将当前切面组件名称作为 KEY，工厂 factory 作为 VALUE 保存到 aspectFactoryCache 缓存中，方便下一次快速解析构建增强器 Advisor；
									已知增强器 Advisor = 切点（Pointcut） + 通知（Advice）组成，其中的 切点（Pointcut）为 AspectJExpressionPointcut 类型，至于通知（Advice）则各不相同，如下所示：
									@Around -> AspectJAroundAdvice，@Before -> AspectJMethodBeforeAdvice，@After -> AspectJAfterAdvice，@AfterReturning -> AspectJAfterReturningAdvice，
									@AfterThrowing -> AspectJAfterThrowingAdvice
								 */
								List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
								if (this.beanFactory.isSingleton(beanName)) {
									// 如果当前切面组件是单例的，则将当前切面组件名称作为 KEY，由通知方法构建成的增强器集合作为 VALUE 保存到 advisorsCache 缓存中；
									this.advisorsCache.put(beanName, classAdvisors);
								} else {
									// 如果当前切面组件不是单例的，则将当前切面组件名称作为 KEY，工厂 factory 作为 VALUE 保存到 aspectFactoryCache 缓存中，方便下一次快速解析构建增强器 Advisor；
									this.aspectFactoryCache.put(beanName, factory);
								}
								advisors.addAll(classAdvisors);
							} else {
								// Per target or per this.
								if (this.beanFactory.isSingleton(beanName)) {
									throw new IllegalArgumentException("Bean with name '" + beanName +
											"' is a singleton, but aspect instantiation model is not singleton");
								}
								MetadataAwareAspectInstanceFactory factory =
										new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
								this.aspectFactoryCache.put(beanName, factory);
								advisors.addAll(this.advisorFactory.getAdvisors(factory));
							}
						}
					}
					// 将从容器中筛选出来的切面组件名称赋值给 aspectBeanNames 缓存，方便下一次直接从缓存中获取，而无需再次从容器中筛选
					this.aspectBeanNames = aspectNames;
					// 返回构建好的增强器集合
					return advisors;
				}
			}
		}

		if (aspectNames.isEmpty()) {
			return Collections.emptyList();
		}
		List<Advisor> advisors = new ArrayList<>();
		// 走到这一步，说明已经经历过上面的步骤：从容器中筛选出切面组件并将切面中的通知方法构建成增强器，此处直接遍历切面组件名称 aspectNames 缓存即可！
		for (String aspectName : aspectNames) {
			// 从缓存中取出当前正在遍历的切面组件名称所对应的增强器集合
			List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
			if (cachedAdvisors != null) {
				advisors.addAll(cachedAdvisors);
			} else {
				/*
					如果 advisorsCache 缓存中不存在的话，说明当前正在遍历的切面组件不是单例的，需要从 aspectFactoryCache 缓存中取出工厂对象 factory，再利用工厂去构建新的增强器集合
					与前面的保存到缓存中的代码逻辑相呼应！
				 */
				MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
				advisors.addAll(this.advisorFactory.getAdvisors(factory));
			}
		}
		return advisors;
	}

	/**
	 * Return whether the aspect bean with the given name is eligible.
	 *
	 * @param beanName the name of the aspect bean
	 * @return whether the bean is eligible
	 */
	protected boolean isEligibleBean(String beanName) {
		return true;
	}

}
