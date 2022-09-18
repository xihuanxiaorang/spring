package top.xiaorang.aop.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author liulei
 * @description 主配置类
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/9/16 4:12
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan({"top.xiaorang.aop"})
public class MainConfig {
}
