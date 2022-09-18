package top.xiaorang.mvc.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;

/**
 * @author liulei
 * @description Spring 不扫描 Controller 组件，excludeFilters{Controller.class}
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/9/16 3:16
 */
@ComponentScan(value = "top.xiaorang.mvc", excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ANNOTATION, value = Controller.class)
})
public class MySpringConfig {
	// 这个 Spring 的父容器
}
