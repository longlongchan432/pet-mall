package com.petmall.mapper;

import com.petmall.entity.Coupon;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CouponMapper {
    List<Coupon> selectAvailable(@Param("now") java.util.Date now);
    Coupon selectById(Long id);
    int insert(Coupon coupon);
    int decreaseStock(@Param("id") Long id);
}