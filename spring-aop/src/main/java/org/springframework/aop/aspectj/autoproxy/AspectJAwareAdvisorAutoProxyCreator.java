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

package org.springframework.aop.aspectj.autoproxy;

import org.aopalliance.aop.Advice;
import org.aspectj.util.PartialOrder;
import org.aspectj.util.PartialOrder.PartialComparable;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.aspectj.AspectJPointcutAdvisor;
import org.springframework.aop.aspectj.AspectJProxyUtils;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}
 * subclass that exposes AspectJ's invocation context and understands AspectJ's rules
 * for advice precedence when multiple pieces of advice come from the same aspect.
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AspectJAwareAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator {

	private static final Comparator<Advisor> DEFAULT_PRECEDENCE_COMPARATOR = new AspectJPrecedenceComparator();


	/**
	 * Sort the supplied {@link Advisor} instances according to AspectJ precedence.
	 * <p>If two pieces of advice come from the same aspect, they will have the same
	 * order. Advice from the same aspect is then further ordered according to the
	 * following rules:
	 * <ul>
	 * <li>If either of the pair is <em>after</em> advice, then the advice declared
	 * last gets highest precedence (i.e., runs last).</li>
	 * <li>Otherwise the advice declared first gets highest precedence (i.e., runs
	 * first).</li>
	 * </ul>
	 * <p><b>Important:</b> Advisors are sorted in precedence order, from the highest
	 * precedence to the lowest. "On the way in" to a join point, the highest precedence
	 * advisor should run first. "On the way out" of a join point, the highest
	 * precedence advisor should run last.
	 */
	@Override
	protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
		List<PartiallyComparableAdvisorHolder> partiallyComparableAdvisors = new ArrayList<>(advisors.size());
		for (Advisor advisor : advisors) {
			partiallyComparableAdvisors.add(
					new PartiallyComparableAdvisorHolder(advisor, DEFAULT_PRECEDENCE_COMPARATOR));
		}
		List<PartiallyComparableAdvisorHolder> sorted = PartialOrder.sort(partiallyComparableAdvisors);
		if (sorted != null) {
			List<Advisor> result = new ArrayList<>(advisors.size());
			for (PartiallyComparableAdvisorHolder pcAdvisor : sorted) {
				result.add(pcAdvisor.getAdvisor());
			}
			return result;
		} else {
			return super.sortAdvisors(advisors);
		}
	}

	/**
	 * Add an {@link ExposeInvocationInterceptor} to the beginning of the advice chain.
	 * <p>This additional advice is needed when using AspectJ pointcut expressions
	 * and when using AspectJ-style advice.
	 */
	@Override
	protected void extendAdvisors(List<Advisor> candidateAdvisors) {
		AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(candidateAdvisors);
	}

	@Override
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		// TODO: Consider optimization by caching the list of the aspect names
		/*
			由子类 AnnotationAwareAspectJAutoProxyCreator 进行重写，从容器中获取所有候选的增强器 Advisor 存在如下两种途径：
			1. 从 IoC 容器中获取 Advisor 类型的组件，该方式可以用于兼容 XML 配置文件中配置的 Bean 或者使用 @Component、@Bean 等方式注册到 IoC 容器中的 Advisor 类型组件，
				当使用 @EnableTransactionManagement 开启事务时会往 IoC 容器中注册一个 ProxyTransactionManagementConfiguration 配置类，
				而在该配置类中就通过 @Bean 的方式往 IoC 容器中注册一个 BeanFactoryTransactionAttributeSourceAdvisor 增强器
			2. 解析 IoC 容器中所有标注 @Aspect 注解的组件，将组件中标注 @Around、@Before、@After、@AfterReturning、@AfterThrowing 注解的通知方法构建成增强器 Advisor，然后保存到 advisorsCache 缓存中，
				使用的时候可以直接从缓存中获取，无需再次进行解析！
		 */
		List<Advisor> candidateAdvisors = findCandidateAdvisors();
		// 遍历从容器中获取到的候选增强器 Advisor
		for (Advisor advisor : candidateAdvisors) {
			// 判断当前组件是否为候选增强器中的一员？如果是的话，则返回 true，跳过当前组件，表明当前组件无需代理
			if (advisor instanceof AspectJPointcutAdvisor &&
					((AspectJPointcutAdvisor) advisor).getAspectName().equals(beanName)) {
				return true;
			}
		}
		return super.shouldSkip(beanClass, beanName);
	}


	/**
	 * Implements AspectJ's {@link PartialComparable} interface for defining partial orderings.
	 */
	private static class PartiallyComparableAdvisorHolder implements PartialComparable {

		private final Advisor advisor;

		private final Comparator<Advisor> comparator;

		public PartiallyComparableAdvisorHolder(Advisor advisor, Comparator<Advisor> comparator) {
			this.advisor = advisor;
			this.comparator = comparator;
		}

		@Override
		public int compareTo(Object obj) {
			Advisor otherAdvisor = ((PartiallyComparableAdvisorHolder) obj).advisor;
			return this.comparator.compare(this.advisor, otherAdvisor);
		}

		@Override
		public int fallbackCompareTo(Object obj) {
			return 0;
		}

		public Advisor getAdvisor() {
			return this.advisor;
		}

		@Override
		public String toString() {
			Advice advice = this.advisor.getAdvice();
			StringBuilder sb = new StringBuilder(ClassUtils.getShortName(advice.getClass()));
			boolean appended = false;
			if (this.advisor instanceof Ordered) {
				sb.append(": order = ").append(((Ordered) this.advisor).getOrder());
				appended = true;
			}
			if (advice instanceof AbstractAspectJAdvice) {
				sb.append(!appended ? ": " : ", ");
				AbstractAspectJAdvice ajAdvice = (AbstractAspectJAdvice) advice;
				sb.append("aspect name = ");
				sb.append(ajAdvice.getAspectName());
				sb.append(", declaration order = ");
				sb.append(ajAdvice.getDeclarationOrder());
			}
			return sb.toString();
		}
	}

}
