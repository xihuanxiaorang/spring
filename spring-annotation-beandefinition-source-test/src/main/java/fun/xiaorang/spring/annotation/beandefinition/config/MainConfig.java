package fun.xiaorang.spring.annotation.beandefinition.config;

import fun.xiaorang.spring.annotation.beandefinition.importbean.MyImportBeanDefinitionRegistrar;
import fun.xiaorang.spring.annotation.beandefinition.importbean.MyImportSelector;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/19 0:23
 */
@ComponentScan("fun.xiaorang.spring.annotation.beandefinition")
@Import({MyImportSelector.class, MyImportBeanDefinitionRegistrar.class})
@ImportResource({"applicationContext.xml"})
public class MainConfig {

}
