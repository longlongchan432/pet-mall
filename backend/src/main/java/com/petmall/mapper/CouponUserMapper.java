package com.petmall.mapper;

import com.petmall.entity.CouponUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CouponUserMapper {
    int insert(CouponUser couponUser);
    CouponUser selectById(Long id);
    List<CouponUser> selectByUserId(@Param("userId") Long userId);
    int markUsed(@Param("id") Long id, @Param("orderId") Long orderId);
    // 查某用户是否已领过某券（防重复领取）
    CouponUser selectByUserAndCoupon(@Param("userId") Long userId, @Param("couponId") Long couponId);
}