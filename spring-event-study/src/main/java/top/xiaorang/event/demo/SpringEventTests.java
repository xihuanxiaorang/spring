package top.xiaorang.event.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import top.xiaorang.event.MyEvent;
import top.xiaorang.event.MyEventListener;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/10/19 22:54
 */
public class SpringEventTests {
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringEventTests.class);

	public static void main(String[] args) {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		applicationContext.addApplicationListener(new MyEventListener());
		applicationContext.publishEvent(new MyEvent("成功！"));
		applicationContext.close();
	}
}
