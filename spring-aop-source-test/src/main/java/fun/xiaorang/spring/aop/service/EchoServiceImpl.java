package fun.xiaorang.spring.aop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/29 16:33
 */
@Service
public class EchoServiceImpl implements EchoService {
	private static final Logger LOGGER = LoggerFactory.getLogger(EchoServiceImpl.class);

	public EchoServiceImpl() {
		LOGGER.info("...HelloService创建了...");
	}

	@Override
	public String echo(String str) {
		LOGGER.info("目标方法执行：{}", str);

		// 模拟异常
//		Object o1 = new ArrayList<>(10).get(11);

		return str;
	}
}
