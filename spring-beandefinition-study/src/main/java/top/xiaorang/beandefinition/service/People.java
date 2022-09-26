package top.xiaorang.beandefinition.service;

import org.springframework.stereotype.Component;

/**
 * @author liulei
 * @description
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/9/25 5:08
 */
@Component
public class People {
	private String name = "小白";

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "People{" +
				"name='" + name + '\'' +
				'}';
	}
}
