package fun.xiaorang.spring.circularreference;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://blog.xiaorang.fun">小让的糖果屋</a>  - show me the code
 * @date 2023/5/8 14:28
 */
public class A {
	private B b;
	

	public B getB() {
		return b;
	}

	public void setB(B b) {
		this.b = b;
	}
}
