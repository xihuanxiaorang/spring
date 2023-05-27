/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		// 定义用于存储 "已处理" 的 BeanFactory 后置处理器的集合
		Set<String> processedBeans = new HashSet<>();

		// 判断 BeanFactory 是否是 BeanDefinitionRegistry 接口的实现类，
		// 因为传进来的 beanFactory 是 DefaultListableBeanFactory 类型的实例对象，而 DefaultListableBeanFactory 又实现了 BeanDefinitionRegistry 接口，所以肯定满足条件走 if 逻辑
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			// 定义用于存储普通的 BeanFactoryPostProcessor 后置处理器的集合
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// 定义用于存储 BeanDefinitionRegistryPostProcessor 类型的 BeanFactory 后置处理器集合，其实 BeanDefinitionRegistryPostProcessor 是 BeanFactoryPostProcessor 的子类
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();
			// 遍历通过 AnnotationConfigApplicationContext#addBeanFactoryPostProcessor() 方法手动添加到 BeanFactory 的 BeanFactory 后置处理器的实例对象集合
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				/*
					判断 postProcessor 是不是 BeanDefinitionRegistryPostProcessor 类型的 BeanFactory 后置处理器，
					BeanDefinitionRegistryPostProcessor 对 BeanFactoryPostProcessor 进行扩展，
						在 BeanFactoryPostProcessor 的基础上增加了 postProcessBeanDefinitionRegistry() 方法，用于向 BeanFactory 中注册 BeanDefinition
					如果判断为 true 的话，则直接执行 BeanDefinitionRegistryPostProcessor 实例对象中的 postProcessBeanDefinitionRegistry() 方法，然后再把该实例对象添加到 registryProcessors 集合中
				 */
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					// 强制类型转换，转换成子类 BeanDefinitionRegistryPostProcessor 实例对象
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					// 执行 BeanDefinitionRegistryPostProcessor 实例对象中的 postProcessBeanDefinitionRegistry() 方法
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					// 将 registryProcessor 添加到 registryProcessors 集合中，方便后续统一执行所有 BeanDefinitionRegistryPostProcessor 实例对象中的 postProcessBeanFactory() 方法
					registryProcessors.add(registryProcessor);
				} else {
					// 如果不是 BeanDefinitionRegistryPostProcessor 类型的 BeanFactory 后置处理器的话，则将该 BeanFactory 后置处理器实例对象添加到普通的 regularPostProcessors 集合中
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			// 定义用于存储当前需要处理的 BeanDefinitionRegistryPostProcessor 类型的 BeanFactory 后置处理器的临时集合，每处理完一批，会阶段性地清空一批
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			/*
				从容器中获取所有类型为 BeanDefinitionRegistryPostProcessor 后置处理器的 BeanDefinition 名称，并判断是否实现了优先级（PriorityOrdered）接口
				例如，内置的用于处理配置类的后置处理器 internalConfigurationAnnotationProcessor，类型为 ConfigurationClassPostProcessor
				一般情况下，只会找到一个符合条件的，即用于处理配置类的后置处理器 ConfigurationClassPostProcessor
				此处有一个坑，为什么自己创建了一个既实现 BeanDefinitionRegistryPostProcessor 接口又实现了 PriorityOrdered 接口的类，并标注了 @Component 注解，但是在此处却无法获得呢？
				其实是因为直到这一步，Spring 还没有去执行包扫描呢！当然获取不到被 @Component 注解标注的自定义的 BeanDefinitionRegistryPostProcessor 后置处理器，
					在下方第一个 invokeBeanDefinitionRegistryPostProcessors() 方法中，会去执行 ConfigurationClassPostProcessor 后置处理器中的 postProcessBeanDefinitionRegistry() 时，此时才会进行包扫描
			 */
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					/*
						从容器中获取该后置处理器的实例对象添加到临时集合 currentRegistryProcessors 中
						beanFactory.getBean()：如果是第一次从容器中去获取该后置处理器，则会去创建该后置处理器的单例对象，然后保存到 BeanFactory 的单例池中，此过程会经历完整的 Bean 的生命周期（实例化、属性填充、初始化）
					 */
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 同时把 name 也添加到 "已处理" 的 BeanFactory 后置处理器集合中，后续会根据该集合来判断某个 BeanFactory 后置处理器是否已经执行过
					processedBeans.add(ppName);
				}
			}
			// 对临时集合中的 BeanDefinitionRegistryPostProcessor 后置处理器进行排序（实现了 PriorityOrdered 接口）
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			/*
				将当前临时集合中所有的 BeanDefinitionRegistryPostProcessor 后置处理器添加到 registryProcessors 集合中，
					方便后续统一执行所有 BeanDefinitionRegistryPostProcessor 后置处理器中的 postProcessBeanFactory() 方法
			 */
			registryProcessors.addAll(currentRegistryProcessors);
			/*
				遍历执行当前临时集合中 BeanDefinitionRegistryPostProcessor 后置处理器中的 postProcessBeanDefinitionRegistry() 方法
				最为典型且重要的一个 BeanDefinitionRegistryPostProcessor 后置处理器为 ConfigurationClassPostProcessor，
					该后置处理器专门用于解析配置类，被 @Configuration、@Component、@ComponentScan、@Import、@ImportResource、@Bean 注解标注的类或方法会被解析封装成 BeanDefinition 后注册到 BeanFactory 中
				是 Spring 中热插拔的一种体现，例如，ConfigurationClassPostProcessor 就相当于一个组件，Spring 中很多事情就交给该组件去管理，如果不想用这个组件时，直接把注册组件的那一步去掉就可以了
			 */
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			// 这一轮 BeanDefinitionRegistryPostProcessor 后置处理器中的 postProcessBeanDefinitionRegistry() 方法都执行完成后，清空临时集合，准备开始下一轮
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			// 与上面一段代码的逻辑大体差不多，可以对照着一块进行分析
			// 从容器中获取所有类型为 BeanDefinitionRegistryPostProcessor 后置处理器的 BeanDefinition 名称，并判断是否实现了排序（Ordered）接口
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					// 从容器中获取该后置处理器的实例对象添加到临时集合 currentRegistryProcessors 中
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 同时把 name 也添加到 "已处理" 的 BeanFactory 后置处理器集合中，后续会根据该集合来判断某个 BeanFactory 后置处理器是否已经执行过
					processedBeans.add(ppName);
				}
			}
			// 对临时集合中的 BeanDefinitionRegistryPostProcessor 后置处理器进行排序（实现了 Ordered 接口）
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			/*
				将当前临时集合中所有的 BeanDefinitionRegistryPostProcessor 后置处理器添加到 registryProcessors 集合中，
				方便后续统一执行所有 BeanDefinitionRegistryPostProcessor 后置处理器中的 postProcessBeanFactory() 方法
			 */
			registryProcessors.addAll(currentRegistryProcessors);
			// 遍历执行当前临时集合中 BeanDefinitionRegistryPostProcessor 后置处理器中的 postProcessBeanDefinitionRegistry() 方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			// 清空临时集合，准备开始下一轮
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			// 最后，从容器中获取所有类型为 BeanDefinitionRegistryPostProcessor 后置处理器的 BeanDefinition 名称，没有实现任何优先级和排序接口的情况
			boolean reiterate = true;
			/*
				直到从容器再也获取不到新的 BeanDefinitionRegistryPostProcessor 后置处理器才会退出 while 循环，这是什么情况呢？
				因为 BeanDefinitionRegistryPostProcessor 后置处理器的作用就是向容器中注册 BeanDefinition，而注册到容器中的 BeanDefinition 有可能是 BeanDefinitionRegistryPostProcessor 后置处理器类型，
					类似于这种情况：在 A 后置处理器的 postProcessBeanDefinitionRegistry() 方法中向容器中注册 B 后置处理器（有可能实现了 PriorityOrdered 或者 Ordered 排序接口）的 BeanDefinition，
					Spring 为了处理该情况的出现，才使用 while 循环不断判断是否有新增的 BeanDefinitionRegistryPostProcessor 后置处理器是否没有被执行到，即保证新增的 B 后置处理器也会被执行到，
					直至不再有新的 BeanDefinitionRegistryPostProcessor 后置处理器出现才会退出循环！
			 */
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						// 从容器中获取该后置处理器的实例对象添加到临时集合 currentRegistryProcessors 中
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						// 同时把 name 也添加到 "已处理" 的 BeanFactory 后置处理器集合中，后续会根据该集合来判断某个 BeanFactory 后置处理器是否已经执行过
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				// 对临时集合中的 BeanDefinitionRegistryPostProcessor 后置处理器进行排序（可能实现了 PriorityOrdered 或者 Ordered 排序接口）
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				/*
					将当前临时集合中所有的 BeanDefinitionRegistryPostProcessor 后置处理器添加到 registryProcessors 集合中，
					方便后续统一执行所有 BeanDefinitionRegistryPostProcessor 后置处理器中的 postProcessBeanFactory() 方法
				 */
				registryProcessors.addAll(currentRegistryProcessors);
				// 遍历执行当前临时集合中 BeanDefinitionRegistryPostProcessor 后置处理器中的 postProcessBeanDefinitionRegistry() 方法
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
				// 清空临时集合，准备开始下一轮
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			/*
				统一执行上面所有类型为 BeanDefinitionRegistryPostProcessor 后置处理器中的 postProcessBeanFactory() 方法
				对于 ConfigurationClassPostProcessor 后置处理器中的 postProcessBeanFactory() 方法，主要用于将 FULL 模式配置类所对应的 BeanDefinition 中的 beanClass 替换为 cglib 增强的子类，
					这样在创建该 FULL 模式的配置类的实例对象时，创建出来的是经过 cglib 增强的动态代理类对象
			 */
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			// 遍历执行所有通过 AnnotationConfigApplicationContext#addBeanFactoryPostProcessor() 方法手动添加到容器中的 BeanFactoryPostProcessor 后置处理器中的 postProcessBeanFactory() 方法
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		} else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		/*
			由于 BeanFactoryPostProcessor 后置处理器不会向容器中注册 BeanDefinition，
				所以不需要像上面处理 BeanDefinitionRegistryPostProcessor 后置处理器一样麻烦，每执行完一轮之后还需要从容器中重新取，检查以下是否有新增的后置处理器没有处理，
				防止优先级高的 BeanDefinitionRegistryPostProcessor 后置处理器利用 postProcessBeanDefinitionRegistry() 方法注册到容器中的没有处理过的 BeanDefinitionRegistryPostProcessor 后置处理器被遗漏
			从容器中取出所有类型为 BeanFactoryPostProcessor 的后置处理器，按照是否实现了 PriorityOrdered 接口、Ordered 接口和剩余的三种情况进行分组分别添加到三个集合中
		 */
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// 如果该 BeanFactoryPostProcessor 后置处理器已经处理过的话，则直接跳过，不再重复处理！
				// skip - already processed in first phase above
			} else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				// 如果实现了 PriorityOrdered 接口，则添加到 priorityOrderedPostProcessors 集合中
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			} else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				// 如果实现了 Ordered 接口，则添加到 orderedPostProcessorNames 集合中
				orderedPostProcessorNames.add(ppName);
			} else {
				// 如果 PriorityOrdered 和 Ordered 两个接口都没有实现，则添加到 orderedPostProcessorNames 集合中
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		// 对集合中的 BeanFactoryPostProcessor 后置处理器进行排序（实现了 PriorityOrdered 接口）
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 遍历执行集合中 BeanFactoryPostProcessor 后置处理器中的 postProcessBeanFactory() 方法
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		// 通过 orderedPostProcessorNames 集合中的名称以及 getBean() 方法从容器中获取 BeanFactoryPostProcessor 后置处理器的实例对象添加到 orderedPostProcessors 集合中
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		// 对集合中的 BeanFactoryPostProcessor 后置处理器进行排序（实现了 Ordered 接口）
		sortPostProcessors(orderedPostProcessors, beanFactory);
		// 遍历执行集合中 BeanFactoryPostProcessor 后置处理器中的 postProcessBeanFactory() 方法
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		// 通过 nonOrderedPostProcessors 集合中的名称以及 getBean() 方法从容器中获取 BeanFactoryPostProcessor 后置处理器的实例对象添加到 nonOrderedPostProcessors 集合中
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		// 遍历执行集合中 BeanFactoryPostProcessor 后置处理器中的 postProcessBeanFactory() 方法
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			} else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			} else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			// comparatorToUse 为 AnnotationAwareOrderComparator 类型的实例对象
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry, ApplicationStartup applicationStartup) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanDefRegistry = applicationStartup.start("spring.context.beandef-registry.post-process")
					.tag("postProcessor", postProcessor::toString);
			/*
				最重要的 ConfigurationClassPostProcessor 后置处理器中的 postProcessBeanDefinitionRegistry() 在此处执行，
				 该后置处理器专门用于解析配置类，被 @Configuration、@Component、@ComponentScan、@Import、@ImportResource、@Bean 注解标注的类或方法会被解析封装成 BeanDefinition 后注册到 BeanFactory 中
			 */
			postProcessor.postProcessBeanDefinitionRegistry(registry);
			postProcessBeanDefRegistry.end();
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanFactory = beanFactory.getApplicationStartup().start("spring.context.bean-factory.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanFactory(beanFactory);
			postProcessBeanFactory.end();
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		if (beanFactory instanceof AbstractBeanFactory) {
			// Bulk addition is more efficient against our CopyOnWriteArrayList there
			((AbstractBeanFactory) beanFactory).addBeanPostProcessors(postProcessors);
		} else {
			for (BeanPostProcessor postProcessor : postProcessors) {
				beanFactory.addBeanPostProcessor(postProcessor);
			}
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
