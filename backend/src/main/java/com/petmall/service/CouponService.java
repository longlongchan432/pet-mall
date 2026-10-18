package com.petmall.service;

import com.petmall.entity.Coupon;
import com.petmall.entity.CouponUser;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CouponService {
    List<Coupon> getAvailableCoupons();
    boolean addCoupon(Coupon coupon);
    List<Map<String, Object>> getMyCoupons(Long userId);
    boolean receiveCoupon(Long couponId, Long userId);
    // 计算券应付金额，券不可用返回原金额
    BigDecimal applyCoupon(BigDecimal amount, Long couponUserId, Long userId);
    // 支付成功后标记券已用
    void useCoupon(Long couponUserId, Long orderId);
}