package top.xiaorang.importbean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import top.xiaorang.importbean.entity.Student;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/10/24 5:34
 */
@ContextConfiguration(locations = "classpath:applicationContext.xml")
@ExtendWith(SpringExtension.class)
public class SpringImportBeanByXmlTests {
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringImportBeanByXmlTests.class);

	@Test
	public void testXml(ApplicationContext applicationContext) {
		Student student = applicationContext.getBean(Student.class);
		LOGGER.info(student.toString());
	}
}
