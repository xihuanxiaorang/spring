package top.xiaorang.mvc;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;
import top.xiaorang.mvc.config.MySpringConfig;
import top.xiaorang.mvc.config.MySpringMVCConfig;

/**
 * @author liulei
 * @description 最快速的整合注解版 SpringMVC 和 Spring
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/9/16 3:20
 */
public class AppQuickStater extends AbstractAnnotationConfigDispatcherServletInitializer {
	/**
	 * 获取根容器的配置（Spring 的配置文件 ===> Spring 的配置类 ===> MySpringConfig.class）
	 */
	@Override
	protected Class<?>[] getRootConfigClasses() {
		return new Class<?>[]{MySpringConfig.class};
	}

	/**
	 * 获取 Web 容器的配置（SpringMVC 的配置文件 ===> SpringMVC 的配置类 ===> MySpringMVCConfig.class）
	 */
	@Override
	protected Class<?>[] getServletConfigClasses() {
		return new Class<?>[]{MySpringMVCConfig.class};
	}

	/**
	 * Servlet 的映射,DispatcherServlet 的映射路径
	 */
	@Override
	protected String[] getServletMappings() {
		return new String[]{"/"};
	}
}
