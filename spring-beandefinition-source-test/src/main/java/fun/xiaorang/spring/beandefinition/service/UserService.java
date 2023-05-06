package fun.xiaorang.spring.beandefinition.service;

import org.springframework.stereotype.Component;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/5 19:49
 */
@Component
public class UserService {
	public String getUserInfo(String username) {
		return username + "用户的详细信息";
	}
}
