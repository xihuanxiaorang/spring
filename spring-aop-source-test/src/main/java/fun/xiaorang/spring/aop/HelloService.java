package fun.xiaorang.spring.aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/4/26 2:28
 */
@Service
public class HelloService {
	private static final Logger LOGGER = LoggerFactory.getLogger(HelloService.class);

	public HelloService() {
		LOGGER.info("...HelloService创建了...");
	}

	/**
	 * 切面目标方法
	 */
	public String sayHello(String name) {
		LOGGER.info("目标方法执行：你好，{}", name);

		// 模拟异常
//		Object o1 = new ArrayList<>(10).get(11);

		return "你好，返回通知";
	}
}