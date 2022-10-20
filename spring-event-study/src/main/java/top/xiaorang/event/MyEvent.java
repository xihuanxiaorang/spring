package top.xiaorang.event;

import org.springframework.context.ApplicationEvent;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/10/19 22:55
 */
public class MyEvent extends ApplicationEvent {
	private static final long serialVersionUID = -7898050348071234064L;

	public MyEvent(Object source) {
		super(source);
	}
}
