package top.xiaorang.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/10/19 23:16
 */
@Component
public class MyApplicationEventListener implements ApplicationListener<ApplicationEvent> {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyApplicationEventListener.class);

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		LOGGER.info("接收到事件：{}", event);
	}
}
