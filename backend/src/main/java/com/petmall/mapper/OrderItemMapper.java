package com.petmall.mapper;

import com.petmall.entity.OrderItem;
import java.util.List;

public interface OrderItemMapper {
    List<OrderItem> selectByOrderId(Long orderId);
    List<OrderItem> selectByOrderNo(String orderNo);
    int insert(OrderItem orderItem);
    int insertBatch(List<OrderItem> orderItems);
}



