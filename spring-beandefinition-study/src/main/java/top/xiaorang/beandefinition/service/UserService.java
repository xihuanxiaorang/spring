package top.xiaorang.beandefinition.service;

/**
 * @author liulei
 * @description
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/9/24 15:13
 */
public class UserService {
	private String username;

	/**
	 * 根据用户名称查询用户详细信息
	 *
	 * @return 用户详细信息
	 */
	public String queryUserInfo() {
		return username + "用户的详细信息";
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
