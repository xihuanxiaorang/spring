package top.xiaorang.importbean.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import top.xiaorang.importbean.entity.Color;
import top.xiaorang.importbean.entity.Student;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/10/25 0:54
 */
@Configuration
@ComponentScan("top.xiaorang.importbean")
@Import({Color.class, MyImportSelector.class, MyImportBeanDefinitionRegistrar.class})
public class MainConfig {
	@Bean
	public Student student() {
		return new Student("xiaobai", 27);
	}
}
