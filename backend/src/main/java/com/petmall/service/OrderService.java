package com.petmall.service;

import com.petmall.entity.Order;
import java.util.List;

public interface OrderService {
    Order createOrder(Order order);
    Order getOrderById(Long id);
    Order getOrderByOrderNo(String orderNo);
    List<Order> getUserOrders(Long userId);
    List<Order> getMerchantOrders(Long merchantId);
    List<Order> getAllOrders(Integer status);
    boolean payOrder(Long orderId, Integer paymentMethod, String password, Long couponUserId);
    boolean shipOrder(Long orderId, String logisticsNo, String logisticsCompany);
    boolean receiveOrder(Long orderId);
    boolean cancelOrder(Long orderId);
    boolean refundOrder(Long orderId);
    boolean processRefund(Long orderId, boolean agree);
}



