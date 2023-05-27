package fun.xiaorang.spring.annotation.beandefinition.pojo;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/27 20:41
 */
@Component
public class Animal {
	public static class Bird {
		@Bean
		public Eagle eagle() {
			return new Eagle();
		}
	}

	@Component
	public static class Tiger {

	}
}
