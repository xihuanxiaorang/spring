package top.xiaorang.importbean.controller;

import org.springframework.stereotype.Controller;
import top.xiaorang.importbean.service.OrderService;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/10/25 1:19
 */
@Controller
public class OrderController {
	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@Override
	public String toString() {
		return "OrderController{" +
				"orderService=" + orderService +
				'}';
	}
}
