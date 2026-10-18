package com.petmall.service.impl;

import com.petmall.entity.Order;
import com.petmall.entity.OrderItem;
import com.petmall.entity.Product;
import com.petmall.mapper.CartMapper;
import com.petmall.mapper.OrderItemMapper;
import com.petmall.mapper.OrderMapper;
import com.petmall.mapper.PaymentMapper;
import com.petmall.mapper.ProductMapper;
import com.petmall.mapper.UserMapper;
import com.petmall.service.CouponService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 状态机并发保护的回归测试：条件更新命中 0 行时必须失败，不能把库存扣成负数或重复回补。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private CartMapper cartMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrder_failsWhenConcurrentOrderDrainsTheStock() {
        Product product = new Product();
        product.setId(1L);
        product.setName("狗粮");
        product.setPrice(new BigDecimal("100"));
        product.setStock(5);
        product.setMerchantId(2L);

        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(1);

        Order order = new Order();
        order.setUserId(12L);
        order.setOrderItems(Collections.singletonList(item));

        when(productMapper.selectById(1L)).thenReturn(product);
        // 读到的库存够，但真正扣减时已被另一个请求抢空 → 条件更新命中 0 行
        when(productMapper.updateStock(anyLong(), anyInt())).thenReturn(0);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.createOrder(order));
        assertEquals("商品库存不足：狗粮", ex.getMessage());
    }

    @Test
    void createOrder_succeedsWhenStockIsActuallyDeducted() {
        Product product = new Product();
        product.setId(1L);
        product.setName("狗粮");
        product.setPrice(new BigDecimal("100"));
        product.setStock(5);
        product.setMerchantId(2L);

        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(1);

        Order order = new Order();
        order.setUserId(12L);
        order.setOrderItems(Collections.singletonList(item));

        when(productMapper.selectById(1L)).thenReturn(product);
        when(productMapper.updateStock(anyLong(), anyInt())).thenReturn(1);

        Order created = orderService.createOrder(order);
        assertEquals(new BigDecimal("100"), created.getTotalAmount());
    }

    @Test
    void payOrder_failsWhenOrderWasCancelledByTimeoutTask() {
        Order order = new Order();
        order.setId(7L);
        order.setUserId(12L);
        order.setOrderNo("PT1");
        order.setStatus(0);
        order.setTotalAmount(new BigDecimal("100"));

        when(orderMapper.selectById(7L)).thenReturn(order);
        when(couponService.applyCoupon(any(), any(), any())).thenReturn(new BigDecimal("100"));
        // 超时任务已把它改成 4（已取消）→ 条件更新命中 0 行，必须抛异常回滚余额与券
        when(orderMapper.payIfUnpaid(anyLong(), any(Date.class))).thenReturn(0);

        assertThrows(RuntimeException.class, () -> orderService.payOrder(7L, null, null, null));
    }
}
