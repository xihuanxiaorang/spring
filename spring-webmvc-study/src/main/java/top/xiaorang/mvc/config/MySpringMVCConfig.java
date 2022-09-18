package top.xiaorang.mvc.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;

/**
 * @author liulei
 * @description SpringMVC 只扫描 Controller 组件,这样形成了父子容器。includeFilters{Controller.class}
 * 也可以不指定父容器类，让 MVC 扫描所有，也就没有父子容器了，只有一个容器，这样 @Component + @RequestMapping 就生效了。
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/9/16 3:17
 */
@ComponentScan(value = "top.xiaorang.mvc", includeFilters = {
		@ComponentScan.Filter(type = FilterType.ANNOTATION, value = Controller.class)
}, useDefaultFilters = false)
public class MySpringMVCConfig {
}
