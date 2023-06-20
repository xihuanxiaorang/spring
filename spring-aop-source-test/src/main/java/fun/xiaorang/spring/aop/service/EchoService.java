package fun.xiaorang.spring.aop.service;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/31 3:14
 */
public interface EchoService {
	/**
	 * 目标方法：原样返回传递的字符串
	 *
	 * @param str 传递的字符串
	 * @return 原样返回传递的字符串
	 */
	String echo(String str);
}
