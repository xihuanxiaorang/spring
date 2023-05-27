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

package org.springframework.context.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.annotation.DeferredImportSelector.Group;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.Predicate;

/**
 * Parses a {@link Configuration} class definition, populating a collection of
 * {@link ConfigurationClass} objects (parsing a single Configuration class may result in
 * any number of ConfigurationClass objects because one Configuration class may import
 * another using the {@link Import} annotation).
 *
 * <p>This class helps separate the concern of parsing the structure of a Configuration
 * class from the concern of registering BeanDefinition objects based on the content of
 * that model (with the exception of {@code @ComponentScan} annotations which need to be
 * registered immediately).
 *
 * <p>This ASM-based implementation avoids reflection and eager class loading in order to
 * interoperate effectively with lazy class loading in a Spring ApplicationContext.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @see ConfigurationClassBeanDefinitionReader
 * @since 3.0
 */
class ConfigurationClassParser {

	private static final PropertySourceFactory DEFAULT_PROPERTY_SOURCE_FACTORY = new DefaultPropertySourceFactory();

	private static final Predicate<String> DEFAULT_EXCLUSION_FILTER = className ->
			(className.startsWith("java.lang.annotation.") || className.startsWith("org.springframework.stereotype."));

	private static final Comparator<DeferredImportSelectorHolder> DEFERRED_IMPORT_COMPARATOR =
			(o1, o2) -> AnnotationAwareOrderComparator.INSTANCE.compare(o1.getImportSelector(), o2.getImportSelector());


	private final Log logger = LogFactory.getLog(getClass());

	private final MetadataReaderFactory metadataReaderFactory;

	private final ProblemReporter problemReporter;

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	private final BeanDefinitionRegistry registry;

	private final ComponentScanAnnotationParser componentScanParser;

	private final ConditionEvaluator conditionEvaluator;

	private final Map<ConfigurationClass, ConfigurationClass> configurationClasses = new LinkedHashMap<>();

	private final Map<String, ConfigurationClass> knownSuperclasses = new HashMap<>();

	private final List<String> propertySourceNames = new ArrayList<>();

	private final ImportStack importStack = new ImportStack();

	private final DeferredImportSelectorHandler deferredImportSelectorHandler = new DeferredImportSelectorHandler();

	private final SourceClass objectSourceClass = new SourceClass(Object.class);


	/**
	 * Create a new {@link ConfigurationClassParser} instance that will be used
	 * to populate the set of configuration classes.
	 */
	public ConfigurationClassParser(MetadataReaderFactory metadataReaderFactory,
									ProblemReporter problemReporter, Environment environment, ResourceLoader resourceLoader,
									BeanNameGenerator componentScanBeanNameGenerator, BeanDefinitionRegistry registry) {

		this.metadataReaderFactory = metadataReaderFactory;
		this.problemReporter = problemReporter;
		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.registry = registry;
		this.componentScanParser = new ComponentScanAnnotationParser(
				environment, resourceLoader, componentScanBeanNameGenerator, registry);
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, resourceLoader);
	}

	/**
	 * 解析从容器中获取到的配置类
	 *
	 * @param configCandidates 配置类集合
	 */
	public void parse(Set<BeanDefinitionHolder> configCandidates) {
		// 遍历配置类集合
		for (BeanDefinitionHolder holder : configCandidates) {
			BeanDefinition bd = holder.getBeanDefinition();
			try {
				if (bd instanceof AnnotatedBeanDefinition) {
					// 解析注解类型的 BeanDefinition，如 @Configuration、@Component、@ComponentScan、@Import、@ImportResource、@Bean 注解标注的类或方法封装而成的 BeanDefinition
					parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
				} else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
					parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
				} else {
					parse(bd.getBeanClassName(), holder.getBeanName());
				}
			} catch (BeanDefinitionStoreException ex) {
				throw ex;
			} catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to parse configuration class [" + bd.getBeanClassName() + "]", ex);
			}
		}

		// 当前批次中的所有配置类处理完成之后，开始集中处理导入进来的 DeferredImportSelector 组件
		this.deferredImportSelectorHandler.process();
	}

	protected final void parse(@Nullable String className, String beanName) throws IOException {
		Assert.notNull(className, "No bean class name for configuration class bean definition");
		MetadataReader reader = this.metadataReaderFactory.getMetadataReader(className);
		processConfigurationClass(new ConfigurationClass(reader, beanName), DEFAULT_EXCLUSION_FILTER);
	}

	protected final void parse(Class<?> clazz, String beanName) throws IOException {
		processConfigurationClass(new ConfigurationClass(clazz, beanName), DEFAULT_EXCLUSION_FILTER);
	}

	protected final void parse(AnnotationMetadata metadata, String beanName) throws IOException {
		// 首先将配置类注解元数据信息和名称包装成一个 ConfigurationClass 对象，然后再进行解析
		processConfigurationClass(new ConfigurationClass(metadata, beanName), DEFAULT_EXCLUSION_FILTER);
	}

	/**
	 * Validate each {@link ConfigurationClass} object.
	 *
	 * @see ConfigurationClass#validate
	 */
	public void validate() {
		for (ConfigurationClass configClass : this.configurationClasses.keySet()) {
			configClass.validate(this.problemReporter);
		}
	}

	public Set<ConfigurationClass> getConfigurationClasses() {
		return this.configurationClasses.keySet();
	}


	protected void processConfigurationClass(ConfigurationClass configClass, Predicate<String> filter) throws IOException {
		// 根据配置类上标注的 @Conditional 注解判断是否需要跳过该配置类的解析，如果配置类上没有标注 @Conditional 注解的话，则无需理会该判断
		if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
			return;
		}

		/*
			判断当前处理的配置类是否已经存在，即是否已经被解析过？如果存在的话，则接着判断当前处理的配置类是否是被 import 导入的？
			1. 如果是 import 导入进来的，并且已经存在的配置类也是通过 import 导入进来的，则做一个合并；最后忽略当前 import 进来的配置类，直接返回，不需要再处理当前 import 进来的配置类；
			2. 如果不是 import 导入进来的，说明是显示指定的，则删除掉已经存在的（相当于让当前新的配置类去覆盖掉原来/已经存在的配置类），继续后面配置类真正的解析流程；
		 */
		ConfigurationClass existingClass = this.configurationClasses.get(configClass);
		if (existingClass != null) {
			if (configClass.isImported()) {
				if (existingClass.isImported()) {
					existingClass.mergeImportedBy(configClass);
				}
				// Otherwise ignore new imported config class; existing non-imported class overrides it.
				// 忽略新导入的配置类，使用现有的非导入的配置类覆盖它（非导入的配置类优先级更高！）
				return;
			} else {
				// Explicit bean definition found, probably replacing an import.
				// Let's remove the old one and go with the new one.
				this.configurationClasses.remove(configClass);
				this.knownSuperclasses.values().removeIf(configClass::equals);
			}
		}

		// Recursively process the configuration class and its superclass hierarchy.
		// 递归处理配置类及其超类层次结构
		SourceClass sourceClass = asSourceClass(configClass, filter);
		do {
			/*
				核心方法，解析配置类，该方法的返回值为当前正在解析的配置类的父类，如果当前配置类存在父类且其父类也是一个配置类的话，则也会进行解析，递归处理，直至不再存在父类或父类不是配置类为止。
				解析所有标注在配置类上的 @Component、@ComponentScan、@ImportResource、@PropertySource、@Import 等注解或标注在类中方法上的 @Bean 注解
			 */
			sourceClass = doProcessConfigurationClass(configClass, sourceClass, filter);
		}
		while (sourceClass != null);

		// 将解析过的配置类添加到 configurationClasses 集合中
		this.configurationClasses.put(configClass, configClass);
	}

	/**
	 * Apply processing and build a complete {@link ConfigurationClass} by reading the
	 * annotations, members and methods from the source class. This method can be called
	 * multiple times as relevant sources are discovered.
	 * 通过从 sourceClass 中读取注解、成员和方法，应用处理并构建一个完整的 ConfigurationClass 对象
	 *
	 * @param configClass the configuration class being build
	 * @param sourceClass a source class
	 * @return the superclass, or {@code null} if none found or previously processed
	 */
	@Nullable
	protected final SourceClass doProcessConfigurationClass(
			ConfigurationClass configClass, SourceClass sourceClass, Predicate<String> filter)
			throws IOException {

		// 处理 @Component 注解
		if (configClass.getMetadata().isAnnotated(Component.class.getName())) {
			// Recursively process any member (nested) classes first
			/*
				首先递归处理配置类中的内部类，如果当前配置类中存在内部类，并且内部类是一个配置类的话，才会将配置类中的内部类也当作一个配置类去解析，并且标记是配置类的内部类是被导入的（importedBy）
				注意：只有被 @Component 注解标注的配置类才会去检查其内部类是不是也是一个配置类
			 */
			processMemberClasses(configClass, sourceClass, filter);
		}

		// Process any @PropertySource annotations
		// 处理 @PropertySource 注解，将 properties 或者 xml 配置文件中的内容解析后存到环境变量 environment 对象中
		for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(
				sourceClass.getMetadata(), PropertySources.class,
				org.springframework.context.annotation.PropertySource.class)) {
			if (this.environment instanceof ConfigurableEnvironment) {
				processPropertySource(propertySource);
			} else {
				logger.info("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +
						"]. Reason: Environment must implement ConfigurableEnvironment");
			}
		}

		// Process any @ComponentScan annotations
		// 处理 @ComponentScan 注解
		// 获取 @ComponentScan 注解中的属性信息，除了最常用的 basePackages，还包括 includeFilters、excludeFilters 等等
		Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
				sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
		if (!componentScans.isEmpty() &&
				!this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
			// 循环处理 @ComponentScan 注解中的属性信息
			for (AnnotationAttributes componentScan : componentScans) {
				// The config class is annotated with @ComponentScan -> perform the scan immediately
				/*
					使用扫描器扫描指定包路径及其子包下所有标注 @Component、@Controller、@Service、@Repository 注解的组件，
					如果在 @ComponentScan 注解中没有指定要扫描的包路径的话，则将当前 @ComponentScan 注解所标注的配置类的包作为要扫描的包路径；
				 */
				Set<BeanDefinitionHolder> scannedBeanDefinitions =
						this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
				// Check the set of scanned definitions for any further config classes and parse recursively if needed
				// 遍历扫描到的组件，如果其中某个组件是一个配置类的话，则也需要对其进行递归解析
				for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
					BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
					if (bdCand == null) {
						bdCand = holder.getBeanDefinition();
					}
					// 判断当前扫描到的组件是不是一个配置类
					if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
						// 如果当前扫描到的组件是一个配置类的话，则递归解析该配置类
						parse(bdCand.getBeanClassName(), holder.getBeanName());
					}
				}
			}
		}

		// Process any @Import annotations
		/*
			处理 @Import 注解，具体实现步骤如下：
			1. 首先收集当前配置类上所有希望通过 @Import 注解导入的类，需要考虑配置类上的某个注解是复合注解并且该复合注解中可能存在 @Import 注解的情况；
			2. 将要导入的组件分成以下三种情况：
				a. 如果当前导入的组件是 ImportSelector 接口类型：
					则先通过反射实例化该组件，在此期间，如果判断该组件还实现 BeanClassLoaderAware、BeanFactoryAware、EnvironmentAware、ResourceLoaderAware 等 Aware 接口的话，
						则还会执行相关 Aware 接口中的方法
					判断该组件是否为 ImportSelector 接口的子类 DeferredImportSelector 接口类型，
						如果是的话，则会将当前为 DeferredImportSelector 子接口类型的实例对象添加到当前配置类的 deferredImportSelectorHandler 属性中的 deferredImportSelectors 集合中保存起来，
							等到当前配置类所属批次中的所有配置类处理完成之后再对其进行集中处理，处理逻辑位于 ConfigurationClassParser#parse() 方法中的最后一行代码
						如果不是的话，则立即执行当前 ImportSelector 接口类型的实例对象中的 selectImports() 方法，获取要导入的所有组件的全限定名之后，将这些组件包装成一个个的 SourceClass 对象执行递归导入
				b. 如果当前导入的组件是 ImportBeanDefinitionRegistrar 接口类型：
					则先通过反射实例化该组件，在此期间，如果判断该组件还实现 BeanClassLoaderAware、BeanFactoryAware、EnvironmentAware、ResourceLoaderAware 等 Aware 接口的话，
						则还会执行相关 Aware 接口中的方法
					最后，将 ImportBeanDefinitionRegistrar 接口的实例对象添加到当前配置类的 importBeanDefinitionRegistrars 属性集合中保存起来，
						并没有立即执行 ImportBeanDefinitionRegistrar 实例对象中的 registerBeanDefinitions() 方法
				c. 如果当前导入的组件是一个普通类（相较于上面两种情况而言）：则将其当成是一个配置类进行处理，走配置类的解析的流程

		 */
		processImports(configClass, sourceClass, getImports(sourceClass), filter, true);

		// Process any @ImportResource annotations
		// 处理 @ImportResource 注解，将 @ImportResource 注解中配置文件文件路径添加到当前配置类的 importedResources 属性中保存起来，此时并没有立即对这些配置文件进行处理！
		// 获取 @ImportResource 注解中的属性信息，即配置文件路径
		AnnotationAttributes importResource =
				AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
		if (importResource != null) {
			String[] resources = importResource.getStringArray("locations");
			Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
			for (String resource : resources) {
				// 使用环境变量解析当前配置文件路径中的占位符信息（${...}）
				String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
				// 将配置文件路径添加到当前配置类的 importedResources 属性中保存起来
				configClass.addImportedResource(resolvedResource, readerClass);
			}
		}

		// Process individual @Bean methods
		// 处理 @Bean 注解，找出当前配置类中所有被 @Bean 注解标注的方法后，将这些方法添加到当前配置类的 beanMethods 属性中保存起来，此时并没有立即对这些方法进行处理！
		Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);
		for (MethodMetadata methodMetadata : beanMethods) {
			// 将当前被 @Bean 注解标注的方法添加到当前配置类的 beanMethods 属性中保存起来
			configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
		}

		// Process default methods on interfaces
		// 处理配置类所实现接口中被 @Bean 注解标注的默认方法的情况，与上面一样，会将这些方法添加到当前配置类的 beanMethods 属性中保存起来，此时并没有立即对这些方法进行处理！
		processInterfaces(configClass, sourceClass);

		// Process superclass, if any
		/*
			判断当前配置类是否存在父类？
			a. 如果存在的话，则将其父类当作一个配置类去处理，走配置类的解析流程，外层方法会进行递归处理（因为存在父类的父类的情况）；
			b. 如果不存在的话，则当前配置类的解析流程完成！
		 */
		if (sourceClass.getMetadata().hasSuperClass()) {
			String superclass = sourceClass.getMetadata().getSuperClassName();
			if (superclass != null && !superclass.startsWith("java") &&
					!this.knownSuperclasses.containsKey(superclass)) {
				this.knownSuperclasses.put(superclass, configClass);
				// Superclass found, return its annotation metadata and recurse
				return sourceClass.getSuperClass();
			}
		}

		// No superclass -> processing is complete
		return null;
	}

	/**
	 * Register member (nested) classes that happen to be configuration classes themselves.
	 */
	private void processMemberClasses(ConfigurationClass configClass, SourceClass sourceClass,
									  Predicate<String> filter) throws IOException {
		// 获取配置类中所有的内部类
		Collection<SourceClass> memberClasses = sourceClass.getMemberClasses();
		if (!memberClasses.isEmpty()) {
			List<SourceClass> candidates = new ArrayList<>(memberClasses.size());
			for (SourceClass memberClass : memberClasses) {
				/*
					判断内部类是不是也是一个 LITE 模式的配置类（被 @Component 或 @ComponentScan 或 @Import 或 @ImportResource 注解的类或类中类中存在被 @Bean 注解标注的方法）
					&& 内部类的类名是否不与正在处理的配置类的类名相等
				 */
				if (ConfigurationClassUtils.isConfigurationCandidate(memberClass.getMetadata()) &&
						!memberClass.getMetadata().getClassName().equals(configClass.getMetadata().getClassName())) {
					// 条件成立的话，说明当前内部类是一个配置类，则将当前内部类添加到候选的配置类集合中
					candidates.add(memberClass);
				}
			}
			// 对候选的配置类按照优先级进行排序
			OrderComparator.sort(candidates);
			// 遍历解析是配置类的内部类
			for (SourceClass candidate : candidates) {
				/*
					如果出现循环导入这种情况则直接报错！
					如在配置类 MainConfig 中有一个内部类 A，在内部类 A 标注 @Import 注解，此时说明内部类 A 是一个配置类，但是内部类 A 却用 @Import 注解导入 MainConfig 配置类，此时就出现循环导入的情况
				 */
				if (this.importStack.contains(configClass)) {
					this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
				} else {
					this.importStack.push(configClass);
					try {
						// 递归解析是配置类的内部类，并且标记当前是配置类的内部类是被当前配置类导入的（importedBy）
						processConfigurationClass(candidate.asConfigClass(configClass), filter);
					} finally {
						this.importStack.pop();
					}
				}
			}
		}
	}

	/**
	 * Register default methods on interfaces implemented by the configuration class.
	 */
	private void processInterfaces(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
		for (SourceClass ifc : sourceClass.getInterfaces()) {
			Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(ifc);
			for (MethodMetadata methodMetadata : beanMethods) {
				if (!methodMetadata.isAbstract()) {
					// A default method or other concrete method on a Java 8+ interface...
					configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
				}
			}
			processInterfaces(configClass, ifc);
		}
	}

	/**
	 * Retrieve the metadata for all <code>@Bean</code> methods.
	 */
	private Set<MethodMetadata> retrieveBeanMethodMetadata(SourceClass sourceClass) {
		AnnotationMetadata original = sourceClass.getMetadata();
		Set<MethodMetadata> beanMethods = original.getAnnotatedMethods(Bean.class.getName());
		if (beanMethods.size() > 1 && original instanceof StandardAnnotationMetadata) {
			// Try reading the class file via ASM for deterministic declaration order...
			// Unfortunately, the JVM's standard reflection returns methods in arbitrary
			// order, even between different runs of the same application on the same JVM.
			try {
				AnnotationMetadata asm =
						this.metadataReaderFactory.getMetadataReader(original.getClassName()).getAnnotationMetadata();
				Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Bean.class.getName());
				if (asmMethods.size() >= beanMethods.size()) {
					Set<MethodMetadata> selectedMethods = new LinkedHashSet<>(asmMethods.size());
					for (MethodMetadata asmMethod : asmMethods) {
						for (MethodMetadata beanMethod : beanMethods) {
							if (beanMethod.getMethodName().equals(asmMethod.getMethodName())) {
								selectedMethods.add(beanMethod);
								break;
							}
						}
					}
					if (selectedMethods.size() == beanMethods.size()) {
						// All reflection-detected methods found in ASM method set -> proceed
						beanMethods = selectedMethods;
					}
				}
			} catch (IOException ex) {
				logger.debug("Failed to read class file via ASM for determining @Bean method order", ex);
				// No worries, let's continue with the reflection metadata we started with...
			}
		}
		return beanMethods;
	}


	/**
	 * Process the given <code>@PropertySource</code> annotation metadata.
	 *
	 * @param propertySource metadata for the <code>@PropertySource</code> annotation found
	 * @throws IOException if loading a property source failed
	 */
	private void processPropertySource(AnnotationAttributes propertySource) throws IOException {
		String name = propertySource.getString("name");
		if (!StringUtils.hasLength(name)) {
			name = null;
		}
		String encoding = propertySource.getString("encoding");
		if (!StringUtils.hasLength(encoding)) {
			encoding = null;
		}
		// 获取配置文件所在路径集合，可能会加载多个配置文件中的内容
		String[] locations = propertySource.getStringArray("value");
		Assert.isTrue(locations.length > 0, "At least one @PropertySource(value) location is required");
		boolean ignoreResourceNotFound = propertySource.getBoolean("ignoreResourceNotFound");

		Class<? extends PropertySourceFactory> factoryClass = propertySource.getClass("factory");
		PropertySourceFactory factory = (factoryClass == PropertySourceFactory.class ?
				DEFAULT_PROPERTY_SOURCE_FACTORY : BeanUtils.instantiateClass(factoryClass));

		for (String location : locations) {
			try {
				// 解析配置文件路径中的占位符信息 ${...}
				String resolvedLocation = this.environment.resolveRequiredPlaceholders(location);
				// 获取资源文件
				Resource resource = this.resourceLoader.getResource(resolvedLocation);
				// 将配置文件中的内容解析后存到环境变量 environment 对象中
				addPropertySource(factory.createPropertySource(name, new EncodedResource(resource, encoding)));
			} catch (IllegalArgumentException | FileNotFoundException | UnknownHostException | SocketException ex) {
				// Placeholders not resolvable or resource not found when trying to open it
				if (ignoreResourceNotFound) {
					if (logger.isInfoEnabled()) {
						logger.info("Properties location [" + location + "] not resolvable: " + ex.getMessage());
					}
				} else {
					throw ex;
				}
			}
		}
	}

	private void addPropertySource(PropertySource<?> propertySource) {
		String name = propertySource.getName();
		MutablePropertySources propertySources = ((ConfigurableEnvironment) this.environment).getPropertySources();

		if (this.propertySourceNames.contains(name)) {
			// We've already added a version, we need to extend it
			PropertySource<?> existing = propertySources.get(name);
			if (existing != null) {
				PropertySource<?> newSource = (propertySource instanceof ResourcePropertySource ?
						((ResourcePropertySource) propertySource).withResourceName() : propertySource);
				if (existing instanceof CompositePropertySource) {
					((CompositePropertySource) existing).addFirstPropertySource(newSource);
				} else {
					if (existing instanceof ResourcePropertySource) {
						existing = ((ResourcePropertySource) existing).withResourceName();
					}
					CompositePropertySource composite = new CompositePropertySource(name);
					composite.addPropertySource(newSource);
					composite.addPropertySource(existing);
					propertySources.replace(name, composite);
				}
				return;
			}
		}

		if (this.propertySourceNames.isEmpty()) {
			propertySources.addLast(propertySource);
		} else {
			String firstProcessed = this.propertySourceNames.get(this.propertySourceNames.size() - 1);
			propertySources.addBefore(firstProcessed, propertySource);
		}
		this.propertySourceNames.add(name);
	}


	/**
	 * Returns {@code @Import} class, considering all meta-annotations.
	 * 返回所有要导入的类，需要考虑所有的元注解
	 */
	private Set<SourceClass> getImports(SourceClass sourceClass) throws IOException {
		Set<SourceClass> imports = new LinkedHashSet<>();
		Set<SourceClass> visited = new LinkedHashSet<>();
		collectImports(sourceClass, imports, visited);
		return imports;
	}

	/**
	 * Recursively collect all declared {@code @Import} values. Unlike most
	 * meta-annotations it is valid to have several {@code @Import}s declared with
	 * different values; the usual process of returning values from the first
	 * meta-annotation on a class is not sufficient.
	 * <p>For example, it is common for a {@code @Configuration} class to declare direct
	 * {@code @Import}s in addition to meta-imports originating from an {@code @Enable}
	 * annotation.
	 *
	 * @param sourceClass the class to search
	 * @param imports     the imports collected so far
	 * @param visited     used to track visited classes to prevent infinite recursion
	 * @throws IOException if there is any problem reading metadata from the named class
	 */
	private void collectImports(SourceClass sourceClass, Set<SourceClass> imports, Set<SourceClass> visited)
			throws IOException {

		if (visited.add(sourceClass)) {
			// 遍历标注在当前配置类上的所有注解
			for (SourceClass annotation : sourceClass.getAnnotations()) {
				String annName = annotation.getMetadata().getClassName();
				if (!annName.equals(Import.class.getName())) {
					// 如果当前正在遍历的注解不是 @Import 注解，则递归处理，因为存在某个注解是一个复合注解，在该复合注解中可能存在 @Import 注解的情况
					collectImports(annotation, imports, visited);
				}
			}
			// 将所有收集到的要导入的类包装成 SourceClass 对象添加到集合中
			imports.addAll(sourceClass.getAnnotationAttributes(Import.class.getName(), "value"));
		}
	}

	private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,
								Collection<SourceClass> importCandidates, Predicate<String> exclusionFilter,
								boolean checkForCircularImports) {
		// 如果没有需要导入的组件，则直接返回，不再执行后面的逻辑！
		if (importCandidates.isEmpty()) {
			return;
		}

		// 如果出现循环导入这种情况，则直接报错！
		if (checkForCircularImports && isChainedImportOnStack(configClass)) {
			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
		} else {
			this.importStack.push(configClass);
			try {
				// 遍历导入的组件
				for (SourceClass candidate : importCandidates) {
					// 判断当前导入的组件是否为 ImportSelector 接口类型？
					if (candidate.isAssignable(ImportSelector.class)) {
						// Candidate class is an ImportSelector -> delegate to it to determine imports
						/*
							如果当前导入的组件是 ImportSelector 接口类型，
							则先通过反射实例化该组件，在此期间，如果判断该组件还实现 BeanClassLoaderAware、BeanFactoryAware、EnvironmentAware、ResourceLoaderAware 等 Aware 接口的话，
								则还会执行相关 Aware 接口中的方法
							判断该组件是否为 ImportSelector 接口的子类 DeferredImportSelector 接口类型，
								如果是的话，说明推迟导入，则会将当前 DeferredImportSelector 组件添加到当前配置类的 deferredImportSelectorHandler 属性中的 deferredImportSelectors 集合中保存起来，
									等到当前配置类所属批次中的所有配置类处理完成之后再对其进行集中处理；存在另外一种情况：如果当前 DeferredImportSelector 组件是被其他 DeferredImportSelector 组件导入进来的，
										则会立即对该组件进行处理，而不用再等到后面去进行集中处理
									应用：SpringBoot 中的自动配置就是使用这种方式向 IoC 容器中导入大量的自动配置类（xxxAutoConfiguration）组件，
											标注在 SpringBoot 启动类上的 @SpringBootApplication => @EnableAutoConfiguration => @Import({AutoConfigurationImportSelector.class})，
											其中的 AutoConfigurationImportSelector 就是一个 DeferredImportSelector 接口类型的组件
								如果不是的话，则立即执行当前 ImportSelector 组件中的 selectImports() 方法，获取要导入的所有组件的全限定名之后，将这些组件包装成一个个的 SourceClass 对象执行递归导入
						 */
						Class<?> candidateClass = candidate.loadClass();
						ImportSelector selector = ParserStrategyUtils.instantiateClass(candidateClass, ImportSelector.class,
								this.environment, this.resourceLoader, this.registry);
						Predicate<String> selectorFilter = selector.getExclusionFilter();
						if (selectorFilter != null) {
							exclusionFilter = exclusionFilter.or(selectorFilter);
						}
						if (selector instanceof DeferredImportSelector) {
							this.deferredImportSelectorHandler.handle(configClass, (DeferredImportSelector) selector);
						} else {
							String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
							Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames, exclusionFilter);
							processImports(configClass, currentSourceClass, importSourceClasses, exclusionFilter, false);
						}
					}
					// 判断当前导入的组件是否为 ImportBeanDefinitionRegistrar 接口类型？
					else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
						// Candidate class is an ImportBeanDefinitionRegistrar ->
						// delegate to it to register additional bean definitions
						/*
							如果当前导入的组件是 ImportBeanDefinitionRegistrar 接口类型，
							则先通过反射实例化该组件，在此期间，如果判断该组件还实现 BeanClassLoaderAware、BeanFactoryAware、EnvironmentAware、ResourceLoaderAware 等 Aware 接口的话，
								则还会执行相关 Aware 接口中的方法
							最后，将 ImportBeanDefinitionRegistrar 接口的实例对象添加到当前配置类的 importBeanDefinitionRegistrars 属性集合中保存起来，
								并没有立即执行 ImportBeanDefinitionRegistrar 实例对象中的 registerBeanDefinitions() 方法
						 */
						Class<?> candidateClass = candidate.loadClass();
						ImportBeanDefinitionRegistrar registrar =
								ParserStrategyUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class,
										this.environment, this.resourceLoader, this.registry);
						configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
					} else {
						// Candidate class not an ImportSelector or ImportBeanDefinitionRegistrar ->
						// process it as an @Configuration class
						// 如果当前导入的组件是一个普通类（相较于上面两种情况而言）的话，则将其当成是一个配置类进行处理，走配置类的解析的流程，并且标记该普通类所对应的配置类是由当前配置类导入的（importedBy）
						this.importStack.registerImport(
								currentSourceClass.getMetadata(), candidate.getMetadata().getClassName());
						processConfigurationClass(candidate.asConfigClass(configClass), exclusionFilter);
					}
				}
			} catch (BeanDefinitionStoreException ex) {
				throw ex;
			} catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to process import candidates for configuration class [" +
								configClass.getMetadata().getClassName() + "]", ex);
			} finally {
				this.importStack.pop();
			}
		}
	}

	private boolean isChainedImportOnStack(ConfigurationClass configClass) {
		if (this.importStack.contains(configClass)) {
			String configClassName = configClass.getMetadata().getClassName();
			AnnotationMetadata importingClass = this.importStack.getImportingClassFor(configClassName);
			while (importingClass != null) {
				if (configClassName.equals(importingClass.getClassName())) {
					return true;
				}
				importingClass = this.importStack.getImportingClassFor(importingClass.getClassName());
			}
		}
		return false;
	}

	ImportRegistry getImportRegistry() {
		return this.importStack;
	}


	/**
	 * Factory method to obtain a {@link SourceClass} from a {@link ConfigurationClass}.
	 */
	private SourceClass asSourceClass(ConfigurationClass configurationClass, Predicate<String> filter) throws IOException {
		AnnotationMetadata metadata = configurationClass.getMetadata();
		if (metadata instanceof StandardAnnotationMetadata) {
			return asSourceClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass(), filter);
		}
		return asSourceClass(metadata.getClassName(), filter);
	}

	/**
	 * Factory method to obtain a {@link SourceClass} from a {@link Class}.
	 */
	SourceClass asSourceClass(@Nullable Class<?> classType, Predicate<String> filter) throws IOException {
		if (classType == null || filter.test(classType.getName())) {
			return this.objectSourceClass;
		}
		try {
			// Sanity test that we can reflectively read annotations,
			// including Class attributes; if not -> fall back to ASM
			for (Annotation ann : classType.getDeclaredAnnotations()) {
				AnnotationUtils.validateAnnotation(ann);
			}
			return new SourceClass(classType);
		} catch (Throwable ex) {
			// Enforce ASM via class name resolution
			return asSourceClass(classType.getName(), filter);
		}
	}

	/**
	 * Factory method to obtain a {@link SourceClass} collection from class names.
	 */
	private Collection<SourceClass> asSourceClasses(String[] classNames, Predicate<String> filter) throws IOException {
		List<SourceClass> annotatedClasses = new ArrayList<>(classNames.length);
		for (String className : classNames) {
			annotatedClasses.add(asSourceClass(className, filter));
		}
		return annotatedClasses;
	}

	/**
	 * Factory method to obtain a {@link SourceClass} from a class name.
	 */
	@SuppressWarnings("deprecation")
	SourceClass asSourceClass(@Nullable String className, Predicate<String> filter) throws IOException {
		if (className == null || filter.test(className)) {
			return this.objectSourceClass;
		}
		if (className.startsWith("java")) {
			// Never use ASM for core java types
			try {
				return new SourceClass(ClassUtils.forName(className, this.resourceLoader.getClassLoader()));
			} catch (ClassNotFoundException ex) {
				throw new org.springframework.core.NestedIOException("Failed to load class [" + className + "]", ex);
			}
		}
		return new SourceClass(this.metadataReaderFactory.getMetadataReader(className));
	}


	@SuppressWarnings("serial")
	private static class ImportStack extends ArrayDeque<ConfigurationClass> implements ImportRegistry {

		private final MultiValueMap<String, AnnotationMetadata> imports = new LinkedMultiValueMap<>();

		public void registerImport(AnnotationMetadata importingClass, String importedClass) {
			this.imports.add(importedClass, importingClass);
		}

		@Override
		@Nullable
		public AnnotationMetadata getImportingClassFor(String importedClass) {
			return CollectionUtils.lastElement(this.imports.get(importedClass));
		}

		@Override
		public void removeImportingClass(String importingClass) {
			for (List<AnnotationMetadata> list : this.imports.values()) {
				for (Iterator<AnnotationMetadata> iterator = list.iterator(); iterator.hasNext(); ) {
					if (iterator.next().getClassName().equals(importingClass)) {
						iterator.remove();
						break;
					}
				}
			}
		}

		/**
		 * Given a stack containing (in order)
		 * <ul>
		 * <li>com.acme.Foo</li>
		 * <li>com.acme.Bar</li>
		 * <li>com.acme.Baz</li>
		 * </ul>
		 * return "[Foo->Bar->Baz]".
		 */
		@Override
		public String toString() {
			StringJoiner joiner = new StringJoiner("->", "[", "]");
			for (ConfigurationClass configurationClass : this) {
				joiner.add(configurationClass.getSimpleName());
			}
			return joiner.toString();
		}
	}

	private static class DeferredImportSelectorHolder {

		private final ConfigurationClass configurationClass;

		private final DeferredImportSelector importSelector;

		public DeferredImportSelectorHolder(ConfigurationClass configClass, DeferredImportSelector selector) {
			this.configurationClass = configClass;
			this.importSelector = selector;
		}

		public ConfigurationClass getConfigurationClass() {
			return this.configurationClass;
		}

		public DeferredImportSelector getImportSelector() {
			return this.importSelector;
		}
	}

	private static class DeferredImportSelectorGrouping {

		private final DeferredImportSelector.Group group;

		private final List<DeferredImportSelectorHolder> deferredImports = new ArrayList<>();

		DeferredImportSelectorGrouping(Group group) {
			this.group = group;
		}

		public void add(DeferredImportSelectorHolder deferredImport) {
			this.deferredImports.add(deferredImport);
		}

		/**
		 * Return the imports defined by the group.
		 *
		 * @return each import with its associated configuration class
		 */
		public Iterable<Group.Entry> getImports() {
			for (DeferredImportSelectorHolder deferredImport : this.deferredImports) {
				this.group.process(deferredImport.getConfigurationClass().getMetadata(),
						deferredImport.getImportSelector());
			}
			return this.group.selectImports();
		}

		public Predicate<String> getCandidateFilter() {
			Predicate<String> mergedFilter = DEFAULT_EXCLUSION_FILTER;
			for (DeferredImportSelectorHolder deferredImport : this.deferredImports) {
				Predicate<String> selectorFilter = deferredImport.getImportSelector().getExclusionFilter();
				if (selectorFilter != null) {
					mergedFilter = mergedFilter.or(selectorFilter);
				}
			}
			return mergedFilter;
		}
	}

	private static class DefaultDeferredImportSelectorGroup implements Group {

		private final List<Entry> imports = new ArrayList<>();

		@Override
		public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
			for (String importClassName : selector.selectImports(metadata)) {
				this.imports.add(new Entry(metadata, importClassName));
			}
		}

		@Override
		public Iterable<Entry> selectImports() {
			return this.imports;
		}
	}

	/**
	 * {@link Problem} registered upon detection of a circular {@link Import}.
	 */
	private static class CircularImportProblem extends Problem {

		public CircularImportProblem(ConfigurationClass attemptedImport, Deque<ConfigurationClass> importStack) {
			super(String.format("A circular @Import has been detected: " +
									"Illegal attempt by @Configuration class '%s' to import class '%s' as '%s' is " +
									"already present in the current import stack %s", importStack.element().getSimpleName(),
							attemptedImport.getSimpleName(), attemptedImport.getSimpleName(), importStack),
					new Location(importStack.element().getResource(), attemptedImport.getMetadata()));
		}
	}

	private class DeferredImportSelectorHandler {

		@Nullable
		private List<DeferredImportSelectorHolder> deferredImportSelectors = new ArrayList<>();

		/**
		 * Handle the specified {@link DeferredImportSelector}. If deferred import
		 * selectors are being collected, this registers this instance to the list. If
		 * they are being processed, the {@link DeferredImportSelector} is also processed
		 * immediately according to its {@link DeferredImportSelector.Group}.
		 *
		 * @param configClass    the source configuration class
		 * @param importSelector the selector to handle
		 */
		public void handle(ConfigurationClass configClass, DeferredImportSelector importSelector) {
			// 将 ConfigurationClass 和 DeferredImportSelector 包装成一个 DeferredImportSelectorHolder 对象
			// 为了方便理解，直接将 DeferredImportSelectorHolder 看成是 DeferredImportSelector
			DeferredImportSelectorHolder holder = new DeferredImportSelectorHolder(configClass, importSelector);
			/*
				判断 deferredImportSelectors 是否为 NULL，其实真正目的是判断当前 DeferredImportSelector 组件不是被其他 DeferredImportSelector 组件导入进来的
					如果是的话，则立即对该组件进行处理，而不用再等到后面去进行集中处理
					如果不是的话，则将该组件添加到 deferredImportSelectors 集合中保存起来，等到当前配置类所属批次中的所有配置类处理完成之后再对其进行集中处理
			 */
			if (this.deferredImportSelectors == null) {
				DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();
				handler.register(holder);
				handler.processGroupImports();
			} else {
				// 添加到 deferredImportSelectors 集合中保存起来
				this.deferredImportSelectors.add(holder);
			}
		}

		public void process() {
			// 获取所有的 DeferredImportSelector 组件
			List<DeferredImportSelectorHolder> deferredImports = this.deferredImportSelectors;
			// 将 deferredImportSelectors 属性置为 NULL，真正意图为：当其他的 DeferredImportSelector 组件被同类型的 DeferredImportSelector 组件导入时，可以立即进行处理，而不用再等到后面去进行集中处理
			this.deferredImportSelectors = null;
			try {
				if (deferredImports != null) {
					DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();
					deferredImports.sort(DEFERRED_IMPORT_COMPARATOR);
					// 将所有的 DeferredImportSelector 组件进行分组注册
					deferredImports.forEach(handler::register);
					// 开始分组处理
					handler.processGroupImports();
				}
			} finally {
				this.deferredImportSelectors = new ArrayList<>();
			}
		}
	}

	private class DeferredImportSelectorGroupingHandler {

		private final Map<Object, DeferredImportSelectorGrouping> groupings = new LinkedHashMap<>();

		private final Map<AnnotationMetadata, ConfigurationClass> configurationClasses = new HashMap<>();

		/**
		 * 分组注册
		 *
		 * @param deferredImport
		 */
		public void register(DeferredImportSelectorHolder deferredImport) {
			/*
			  1. 获取分组条件：首先判断当前 DeferredImportSelector 组件是否没有重写 getImportGroup() 方法或者重写了该方法但是方法返回值为 NULL？
				a. 如果是的话，则分组条件为当前 DeferredImportSelector 组件的实例对象，这样的话，当前 DeferredImportSelector 组件不会与其他 DeferredImportSelector 组件分到同一个组，即自己一个人一组；
				b. 如果不是的话，表示当前 DeferredImportSelector 组件重写了 getImportGroup() 方法并且方法返回值不为 NULL，
					则分组条件为该方法的返回值（一个类型为 DeferredImportSelector.Group 接口实现类的 Class 对象），
					此时，如果两个 DeferredImportSelector 组件中的 getImportGroup() 方法返回的是相同类型的 Class 对象，则这两个组件会被放到同一个组中。
					其中 Group 接口的默认实现为 DefaultDeferredImportSelectorGroup ，🌈 如 SpringBoot 为了实现自动配置功能，
						在自定义的 AutoConfigurationImportSelector 类中对 getImportGroup() 方法进行重写并且方法的返回值为自定义的分组接口实现 AutoConfigurationGroup 的 Class 对象。
			  2. 以分组条件作为 KEY 去 groupings（一个 Map 集合） 中查找 VALUE，其中 VALUE 值为 DeferredImportSelectorGrouping 类型的实例对象；
					首先介绍一下 DeferredImportSelectorGrouping 类，在该类有如下两个属性：
						group：Group，判断当前 DeferredImportSelector 组件是否没有重写 getImportGroup() 方法或者重写了该方法但是方法返回值为 NULL？
									如果是的话，则 group 属性值为 DefaultDeferredImportSelectorGroup 类型的实例对象；
									如果不是的话，则  group 属性值为自定义的 Group 接口实现类的实例对象；
						deferredImports：List，用于保存属于同一组的 DeferredImportSelector 组件
					a. 如果能获取到 VALUE 值的话，说明当前 DeferredImportSelector 组件与其他的 DeferredImportSelector 组件属于同一组，
						被一起保存到同一个 DeferredImportSelectorGrouping 实例对象中的 deferredImports 属性集合中；
					b. 如果获取不到的话，说明当前 DeferredImportSelector 组件自己一个人一组，则创建一个 DeferredImportSelectorGrouping 实例对象作为 VALUE，
						该 DeferredImportSelectorGrouping 实例对象中的 group 属性为通过反射实例化出来的 Group 对象，默认为 DefaultDeferredImportSelectorGroup 类型，
						创建完 DeferredImportSelectorGrouping 实例对象之后，会将当前组件添加到该对象中的 deferredImports 属性集合中；
			 */
			Class<? extends Group> group = deferredImport.getImportSelector().getImportGroup();
			DeferredImportSelectorGrouping grouping = this.groupings.computeIfAbsent(
					(group != null ? group : deferredImport),
					key -> new DeferredImportSelectorGrouping(createGroup(group)));
			grouping.add(deferredImport);
			this.configurationClasses.put(deferredImport.getConfigurationClass().getMetadata(),
					deferredImport.getConfigurationClass());
		}

		/**
		 * 分组处理
		 */
		public void processGroupImports() {
			// 分组处理
			for (DeferredImportSelectorGrouping grouping : this.groupings.values()) {
				Predicate<String> exclusionFilter = grouping.getCandidateFilter();
				/*
					遍历每一组，在 getImports() 方法中，会遍历本组内的所有 DeferredImportSelector 组件，
						对本组中的某个 DeferredImportSelector 组件会使用该组对应的 DeferredImportSelector.Group 实例对象中的 process() 方法进行处理；
					 	1. 默认实现 DefaultDeferredImportSelectorGroup 中的 process() 方法具体实现就是直接执行 DeferredImportSelector 组件中的 selectImports() 方法完成组件的导入；
						2. 如果是自定义的 Group 接口实现类对象，🌈 如 SpringBoot 中的 AutoConfigurationGroup，
							其 process() 方法的具体实现就是去读取类路径下所有的 META-INF/spring.factories 配置文件，
							然后根据指定的 KEY = EnableAutoConfiguration 类从这些配置文件中解析出对应的 VALUE 值（其实就是一堆自动配置类的全限定名），
							最后会向容器中导入一堆经过筛选过滤之后符合条件的自动配置类（xxxAutoConfiguration），该实现就是咱们常说的 SpringFactories 机制，它是 Java SPI 设计思想的延伸和扩展；
				 */
				grouping.getImports().forEach(entry -> {
					ConfigurationClass configurationClass = this.configurationClasses.get(entry.getMetadata());
					try {
						// 将导入进来每一个的组件当成是一个配置类走配置类的解析流程（对配置类上的各种注解进行解析）
						processImports(configurationClass, asSourceClass(configurationClass, exclusionFilter),
								Collections.singleton(asSourceClass(entry.getImportClassName(), exclusionFilter)),
								exclusionFilter, false);
					} catch (BeanDefinitionStoreException ex) {
						throw ex;
					} catch (Throwable ex) {
						throw new BeanDefinitionStoreException(
								"Failed to process import candidates for configuration class [" +
										configurationClass.getMetadata().getClassName() + "]", ex);
					}
				});
			}
		}

		private Group createGroup(@Nullable Class<? extends Group> type) {
			Class<? extends Group> effectiveType = (type != null ? type : DefaultDeferredImportSelectorGroup.class);
			return ParserStrategyUtils.instantiateClass(effectiveType, Group.class,
					ConfigurationClassParser.this.environment,
					ConfigurationClassParser.this.resourceLoader,
					ConfigurationClassParser.this.registry);
		}
	}

	/**
	 * Simple wrapper that allows annotated source classes to be dealt with
	 * in a uniform manner, regardless of how they are loaded.
	 */
	private class SourceClass implements Ordered {

		private final Object source;  // Class or MetadataReader

		private final AnnotationMetadata metadata;

		public SourceClass(Object source) {
			this.source = source;
			if (source instanceof Class) {
				this.metadata = AnnotationMetadata.introspect((Class<?>) source);
			} else {
				this.metadata = ((MetadataReader) source).getAnnotationMetadata();
			}
		}

		public final AnnotationMetadata getMetadata() {
			return this.metadata;
		}

		@Override
		public int getOrder() {
			Integer order = ConfigurationClassUtils.getOrder(this.metadata);
			return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
		}

		public Class<?> loadClass() throws ClassNotFoundException {
			if (this.source instanceof Class) {
				return (Class<?>) this.source;
			}
			String className = ((MetadataReader) this.source).getClassMetadata().getClassName();
			return ClassUtils.forName(className, resourceLoader.getClassLoader());
		}

		public boolean isAssignable(Class<?> clazz) throws IOException {
			if (this.source instanceof Class) {
				return clazz.isAssignableFrom((Class<?>) this.source);
			}
			return new AssignableTypeFilter(clazz).match((MetadataReader) this.source, metadataReaderFactory);
		}

		public ConfigurationClass asConfigClass(ConfigurationClass importedBy) {
			if (this.source instanceof Class) {
				return new ConfigurationClass((Class<?>) this.source, importedBy);
			}
			return new ConfigurationClass((MetadataReader) this.source, importedBy);
		}

		public Collection<SourceClass> getMemberClasses() throws IOException {
			Object sourceToProcess = this.source;
			if (sourceToProcess instanceof Class) {
				Class<?> sourceClass = (Class<?>) sourceToProcess;
				try {
					Class<?>[] declaredClasses = sourceClass.getDeclaredClasses();
					List<SourceClass> members = new ArrayList<>(declaredClasses.length);
					for (Class<?> declaredClass : declaredClasses) {
						members.add(asSourceClass(declaredClass, DEFAULT_EXCLUSION_FILTER));
					}
					return members;
				} catch (NoClassDefFoundError err) {
					// getDeclaredClasses() failed because of non-resolvable dependencies
					// -> fall back to ASM below
					sourceToProcess = metadataReaderFactory.getMetadataReader(sourceClass.getName());
				}
			}

			// ASM-based resolution - safe for non-resolvable classes as well
			MetadataReader sourceReader = (MetadataReader) sourceToProcess;
			String[] memberClassNames = sourceReader.getClassMetadata().getMemberClassNames();
			List<SourceClass> members = new ArrayList<>(memberClassNames.length);
			for (String memberClassName : memberClassNames) {
				try {
					members.add(asSourceClass(memberClassName, DEFAULT_EXCLUSION_FILTER));
				} catch (IOException ex) {
					// Let's skip it if it's not resolvable - we're just looking for candidates
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to resolve member class [" + memberClassName +
								"] - not considering it as a configuration class candidate");
					}
				}
			}
			return members;
		}

		public SourceClass getSuperClass() throws IOException {
			if (this.source instanceof Class) {
				return asSourceClass(((Class<?>) this.source).getSuperclass(), DEFAULT_EXCLUSION_FILTER);
			}
			return asSourceClass(
					((MetadataReader) this.source).getClassMetadata().getSuperClassName(), DEFAULT_EXCLUSION_FILTER);
		}

		public Set<SourceClass> getInterfaces() throws IOException {
			Set<SourceClass> result = new LinkedHashSet<>();
			if (this.source instanceof Class) {
				Class<?> sourceClass = (Class<?>) this.source;
				for (Class<?> ifcClass : sourceClass.getInterfaces()) {
					result.add(asSourceClass(ifcClass, DEFAULT_EXCLUSION_FILTER));
				}
			} else {
				for (String className : this.metadata.getInterfaceNames()) {
					result.add(asSourceClass(className, DEFAULT_EXCLUSION_FILTER));
				}
			}
			return result;
		}

		public Set<SourceClass> getAnnotations() {
			Set<SourceClass> result = new LinkedHashSet<>();
			if (this.source instanceof Class) {
				Class<?> sourceClass = (Class<?>) this.source;
				for (Annotation ann : sourceClass.getDeclaredAnnotations()) {
					Class<?> annType = ann.annotationType();
					if (!annType.getName().startsWith("java")) {
						try {
							result.add(asSourceClass(annType, DEFAULT_EXCLUSION_FILTER));
						} catch (Throwable ex) {
							// An annotation not present on the classpath is being ignored
							// by the JVM's class loading -> ignore here as well.
						}
					}
				}
			} else {
				for (String className : this.metadata.getAnnotationTypes()) {
					if (!className.startsWith("java")) {
						try {
							result.add(getRelated(className));
						} catch (Throwable ex) {
							// An annotation not present on the classpath is being ignored
							// by the JVM's class loading -> ignore here as well.
						}
					}
				}
			}
			return result;
		}

		public Collection<SourceClass> getAnnotationAttributes(String annType, String attribute) throws IOException {
			Map<String, Object> annotationAttributes = this.metadata.getAnnotationAttributes(annType, true);
			if (annotationAttributes == null || !annotationAttributes.containsKey(attribute)) {
				return Collections.emptySet();
			}
			String[] classNames = (String[]) annotationAttributes.get(attribute);
			Set<SourceClass> result = new LinkedHashSet<>();
			for (String className : classNames) {
				result.add(getRelated(className));
			}
			return result;
		}

		@SuppressWarnings("deprecation")
		private SourceClass getRelated(String className) throws IOException {
			if (this.source instanceof Class) {
				try {
					Class<?> clazz = ClassUtils.forName(className, ((Class<?>) this.source).getClassLoader());
					return asSourceClass(clazz, DEFAULT_EXCLUSION_FILTER);
				} catch (ClassNotFoundException ex) {
					// Ignore -> fall back to ASM next, except for core java types.
					if (className.startsWith("java")) {
						throw new org.springframework.core.NestedIOException("Failed to load class [" + className + "]", ex);
					}
					return new SourceClass(metadataReaderFactory.getMetadataReader(className));
				}
			}
			return asSourceClass(className, DEFAULT_EXCLUSION_FILTER);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof SourceClass &&
					this.metadata.getClassName().equals(((SourceClass) other).metadata.getClassName())));
		}

		@Override
		public int hashCode() {
			return this.metadata.getClassName().hashCode();
		}

		@Override
		public String toString() {
			return this.metadata.getClassName();
		}
	}

}
