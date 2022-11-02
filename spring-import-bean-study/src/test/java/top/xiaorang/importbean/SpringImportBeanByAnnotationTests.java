package top.xiaorang.importbean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import top.xiaorang.importbean.config.MainConfig;
import top.xiaorang.importbean.controller.OrderController;
import top.xiaorang.importbean.entity.Blue;
import top.xiaorang.importbean.entity.Cat;
import top.xiaorang.importbean.entity.Color;
import top.xiaorang.importbean.entity.Game;
import top.xiaorang.importbean.entity.Rainbow;
import top.xiaorang.importbean.entity.Student;
import top.xiaorang.importbean.entity.User;
import top.xiaorang.importbean.entity.Yellow;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/10/25 0:59
 */
@ContextConfiguration(classes = {MainConfig.class})
@ExtendWith(SpringExtension.class)
public class SpringImportBeanByAnnotationTests {
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringImportBeanByAnnotationTests.class);

	@Test
	public void testConfigurationWithBeanMethod(ApplicationContext applicationContext) {
		Student student = applicationContext.getBean(Student.class);
		LOGGER.info(student.toString());
	}

	@Test
	public void testComponentScanWithComponent(ApplicationContext applicationContext) {
		OrderController orderController = applicationContext.getBean(OrderController.class);
		LOGGER.info(orderController.toString());
	}

	@Test
	public void testFactoryBean(ApplicationContext applicationContext) {
		User user = applicationContext.getBean(User.class);
		LOGGER.info(user.toString());
	}

	@Test
	public void testImportSimpleClass(ApplicationContext applicationContext) {
		Color color = applicationContext.getBean(Color.class);
		LOGGER.info(color.toString());
	}

	@Test
	public void testImportSelector(ApplicationContext applicationContext) {
		Yellow yellow = applicationContext.getBean(Yellow.class);
		LOGGER.info(yellow.toString());
		Blue blue = applicationContext.getBean(Blue.class);
		LOGGER.info(blue.toString());
	}

	@Test
	public void testImportBeanDefinitionRegistrar(ApplicationContext applicationContext) {
		Rainbow rainbow = applicationContext.getBean(Rainbow.class);
		LOGGER.info(rainbow.toString());
	}

	@Test
	public void testBeanDefinitionRegistryPostProcessor(ApplicationContext applicationContext) {
		Cat cat = applicationContext.getBean(Cat.class);
		LOGGER.info(cat.toString());
	}

	@Test
	public void testBeanFactoryPostProcessor(ApplicationContext applicationContext) {
		Game game = applicationContext.getBean(Game.class);
		LOGGER.info(game.toString());
	}
}
