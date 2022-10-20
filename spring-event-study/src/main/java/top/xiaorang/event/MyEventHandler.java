package top.xiaorang.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/10/19 23:04
 */
@Component
public class MyEventHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyEventHandler.class);

	@EventListener(classes = {MyEvent.class})
	public void handleEvent(MyEvent event) {
		LOGGER.info("接收到自定义事件：{}", event);
	}
}
