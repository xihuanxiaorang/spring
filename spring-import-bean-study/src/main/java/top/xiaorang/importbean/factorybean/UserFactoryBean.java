package top.xiaorang.importbean.factorybean;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;
import top.xiaorang.importbean.entity.User;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/10/25 1:45
 */
@Component
public class UserFactoryBean implements FactoryBean<User> {
	@Override
	public User getObject() throws Exception {
		return new User("sanshi", "123456");
	}

	@Override
	public Class<?> getObjectType() {
		return User.class;
	}
}
