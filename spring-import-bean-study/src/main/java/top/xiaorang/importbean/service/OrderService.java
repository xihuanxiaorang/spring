package top.xiaorang.importbean.service;

import org.springframework.stereotype.Service;
import top.xiaorang.importbean.repository.OrderRepository;

/**
 * @author liulei
 * @description <p style = " font-weight:bold ; "><p/>
 * @github <a href="https://github.com/xihuanxiaorang/spring">spring</a>
 * @Copyright 博客：<a href="https://xiaorang.top">小让的糖果屋</a>  - show me the code
 * @date 2022/10/25 1:20
 */
@Service
public class OrderService {
	private final OrderRepository orderRepository;

	public OrderService(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	@Override
	public String toString() {
		return "OrderService{" +
				"orderRepository=" + orderRepository +
				'}';
	}
}
