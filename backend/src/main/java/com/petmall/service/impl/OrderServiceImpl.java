package com.petmall.service.impl;

import com.petmall.entity.*;
import com.petmall.mapper.*;
import com.petmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private PaymentMapper paymentMapper;
    @Autowired
    private com.petmall.mapper.UserMapper userMapper;
    @Autowired
    private com.petmall.service.CouponService couponService;

    // 待支付订单超时时间（分钟），可用 application.yml 的 order.pay.timeout-minutes 调整
    @Value("${order.pay.timeout-minutes:30}")
    private int payTimeoutMinutes;

    @Override
    @Transactional
    public Order createOrder(Order order) {
        String orderNo = "PT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        order.setOrderNo(orderNo);
        order.setStatus(0); // 待支付
        order.setCreateTime(new Date());

        BigDecimal totalAmount = BigDecimal.ZERO;
        Long merchantId = null;
        for (OrderItem item : order.getOrderItems()) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null || product.getStock() < item.getQuantity()) {
                throw new RuntimeException("商品不存在或库存不足");
            }
            if (merchantId == null) {
                merchantId = product.getMerchantId();
            }
            item.setOrderNo(orderNo);
            item.setProductName(product.getName());
            item.setProductImage(product.getImage());
            item.setPrice(product.getPrice());
            item.setSubtotal(product.getPrice().multiply(new BigDecimal(item.getQuantity())));
            item.setCreateTime(new Date());
            totalAmount = totalAmount.add(item.getSubtotal());
        }
        order.setTotalAmount(totalAmount);
        order.setMerchantId(merchantId);

        orderMapper.insert(order);
        // 设置订单ID到每个订单项
        Long orderId = order.getId();
        for (OrderItem item : order.getOrderItems()) {
            item.setOrderId(orderId);
        }
        orderItemMapper.insertBatch(order.getOrderItems());

        // 减少库存：条件更新，返回 0 说明被并发抢空，抛异常让整个下单事务回滚
        for (OrderItem item : order.getOrderItems()) {
            if (productMapper.updateStock(item.getProductId(), -item.getQuantity()) == 0) {
                throw new RuntimeException("商品库存不足：" + item.getProductName());
            }
        }

        // 清空购物车
        cartMapper.deleteByUserId(order.getUserId());

        return order;
    }

    @Override
    public Order getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order != null) {
            order.setOrderItems(orderItemMapper.selectByOrderId(id));
        }
        return order;
    }

    @Override
    public Order getOrderByOrderNo(String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order != null) {
            order.setOrderItems(orderItemMapper.selectByOrderNo(orderNo));
        }
        return order;
    }

    @Override
    public List<Order> getUserOrders(Long userId) {
        List<Order> orders = orderMapper.selectByUserId(userId);
        for (Order order : orders) {
            if (order != null && order.getId() != null) {
                order.setOrderItems(orderItemMapper.selectByOrderId(order.getId()));
            }
        }
        return orders;
    }

    @Override
    public List<Order> getMerchantOrders(Long merchantId) {
        return orderMapper.selectByMerchantId(merchantId);
    }

    @Override
    public List<Order> getAllOrders(Integer status) {
        return orderMapper.selectAll(status);
    }

    @Override
    @Transactional
    public boolean payOrder(Long orderId, Integer paymentMethod, String password, Long couponUserId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 0) {
            return false;
        }
        // 优惠券计算实付
        java.math.BigDecimal paidAmount = couponService.applyCoupon(order.getTotalAmount(), couponUserId, order.getUserId());
        // 4-余额支付：校验并扣减余额
        if (paymentMethod != null && paymentMethod == 4) {
            com.petmall.entity.User user = userMapper.selectById(order.getUserId());
            java.math.BigDecimal balance = user.getBalance() != null ? user.getBalance() : java.math.BigDecimal.ZERO;
            if (balance.compareTo(paidAmount) < 0) {
                throw new RuntimeException("余额不足，请先充值");
            }
            // 验证密码
            if (password == null) {
                throw new RuntimeException("请输入密码");
            }
            // 密码需要MD5加密后比较
            String hashedPassword = org.springframework.util.DigestUtils.md5DigestAsHex(password.getBytes());
            if (!hashedPassword.equals(user.getPassword())) {
                throw new RuntimeException("密码错误");
            }
            userMapper.updateBalance(order.getUserId(), paidAmount.negate());
        }

        Payment payment = new Payment();
        payment.setPaymentNo("PAY" + System.currentTimeMillis());
        payment.setOrderId(orderId);
        payment.setOrderNo(order.getOrderNo());
        payment.setAmount(paidAmount);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(1); // 已支付
        payment.setCreateTime(new Date());
        payment.setPayTime(new Date());
        paymentMapper.insert(payment);

        // 券标记已使用
        couponService.useCoupon(couponUserId, orderId);

        // 条件更新：只有仍处于待支付(0)才置为待发货(1)，与超时取消互斥；失败抛异常回滚已扣余额与券
        if (orderMapper.payIfUnpaid(orderId, new Date()) == 0) {
            throw new RuntimeException("订单已取消或状态已变更，请刷新后重试");
        }
        return true;
    }

    @Override
    public boolean shipOrder(Long orderId, String logisticsNo, String logisticsCompany) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 1) {
            return false;
        }
        order.setStatus(2); // 待收货
        order.setShipTime(new Date());
        order.setUpdateTime(new Date());
        return orderMapper.updateLogistics(orderId, logisticsNo, logisticsCompany) > 0;
    }

    @Override
    @Transactional
    public boolean receiveOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 2) {
            return false;
        }
        order.setStatus(3); // 已完成
        order.setReceiveTime(new Date());
        order.setUpdateTime(new Date());
        
        // 更新商品销量
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                int currentSales = product.getSales() != null ? product.getSales() : 0;
                product.setSales(currentSales + item.getQuantity());
                productMapper.update(product);
            }
        }
        
        return orderMapper.update(order) > 0;
    }

    @Override
    @Transactional
    public boolean cancelOrder(Long orderId) {
        // 条件更新：只有 status<3 才能取消；返回 0 说明已取消/已完成，避免重复回补库存
        if (orderMapper.cancelIfNotFinished(orderId) == 0) {
            return false;
        }

        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : items) {
            productMapper.updateStock(item.getProductId(), item.getQuantity());
        }
        return true;
    }

    /**
     * 定时取消超时未支付的订单并回补库存。
     * ponytail: 进程内单实例调度够用；多实例同时跑也不会重复取消（cancelIfUnpaid 是条件更新），
     * 只是多几轮空查询。真要集群化再上 Quartz/分布式锁。
     */
    @Scheduled(initialDelay = 30_000L, fixedDelay = 60_000L)
    @Transactional
    public int cancelTimeoutOrders() {
        Date deadline = new Date(System.currentTimeMillis() - payTimeoutMinutes * 60_000L);
        int cancelled = 0;
        for (Long id : orderMapper.selectExpiredUnpaidIds(deadline)) {
            if (orderMapper.cancelIfUnpaid(id) == 0) {
                continue; // 已被用户支付或取消
            }
            for (OrderItem item : orderItemMapper.selectByOrderId(id)) {
                productMapper.updateStock(item.getProductId(), item.getQuantity());
            }
            cancelled++;
        }
        return cancelled;
    }

    @Override
    @Transactional
    public boolean refundOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 3) {
            return false;
        }
        order.setStatus(5); // 退款中
        order.setUpdateTime(new Date());
        return orderMapper.update(order) > 0;
    }

    @Override
    @Transactional
    public boolean processRefund(Long orderId, boolean agree) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 5) {
            return false;
        }

        if (agree) {
            // 商家同意退款
            // 1. 将金额退回用户余额
            userMapper.updateBalance(order.getUserId(), order.getTotalAmount());

            // 2. 更新支付记录状态
            Payment payment = paymentMapper.selectByOrderId(orderId);
            if (payment != null) {
                payment.setStatus(2); // 已退款
                paymentMapper.update(payment);
            }

            // 3. 更新订单状态为已退款
            order.setStatus(6); // 已退款
            order.setUpdateTime(new Date());
            orderMapper.update(order);

            // 4. 恢复库存
            List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
            for (OrderItem item : items) {
                productMapper.updateStock(item.getProductId(), item.getQuantity());
            }
        } else {
            // 商家拒绝退款，恢复订单状态为已完成
            order.setStatus(3); // 已完成
            order.setUpdateTime(new Date());
            orderMapper.update(order);
        }
        return true;
    }
}

