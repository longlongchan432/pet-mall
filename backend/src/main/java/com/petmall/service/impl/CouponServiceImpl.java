package com.petmall.service.impl;

import com.petmall.entity.Coupon;
import com.petmall.entity.CouponUser;
import com.petmall.mapper.CouponMapper;
import com.petmall.mapper.CouponUserMapper;
import com.petmall.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CouponServiceImpl implements CouponService {
    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private CouponUserMapper couponUserMapper;

    @Override
    public List<Coupon> getAvailableCoupons() {
        return couponMapper.selectAvailable(new Date());
    }

    @Override
    public boolean addCoupon(Coupon coupon) {
        if (coupon.getStatus() == null) coupon.setStatus(1);
        if (coupon.getStock() == null) coupon.setStock(coupon.getTotal());
        Date now = new Date();
        coupon.setCreateTime(now);
        coupon.setUpdateTime(now);
        return couponMapper.insert(coupon) > 0;
    }

    @Override
    public List<Map<String, Object>> getMyCoupons(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (CouponUser cu : couponUserMapper.selectByUserId(userId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", cu.getId());
            m.put("status", cu.getStatus());
            m.put("receiveTime", cu.getReceiveTime());
            Coupon c = couponMapper.selectById(cu.getCouponId());
            if (c == null) continue;
            m.put("name", c.getName());
            m.put("type", c.getType());
            m.put("threshold", c.getThreshold());
            m.put("value", c.getValue());
            result.add(m);
        }
        return result;
    }

    @Override
    @Transactional
    public boolean receiveCoupon(Long couponId, Long userId) {
        Coupon c = couponMapper.selectById(couponId);
        if (c == null || c.getStatus() != 1 || c.getStock() <= 0) {
            throw new RuntimeException("优惠券不存在或已领完");
        }
        Date now = new Date();
        if (c.getStartTime() != null && now.before(c.getStartTime())) throw new RuntimeException("活动未开始");
        if (c.getEndTime() != null && now.after(c.getEndTime())) throw new RuntimeException("活动已结束");
        if (couponUserMapper.selectByUserAndCoupon(userId, couponId) != null) {
            throw new RuntimeException("你已领取过该券");
        }
        if (couponMapper.decreaseStock(couponId) <= 0) {
            throw new RuntimeException("手慢啦，已抢光");
        }
        CouponUser cu = new CouponUser();
        cu.setCouponId(couponId);
        cu.setUserId(userId);
        cu.setStatus(0);
        cu.setReceiveTime(now);
        cu.setCreateTime(now);
        cu.setUpdateTime(now);
        return couponUserMapper.insert(cu) > 0;
    }

    @Override
    public BigDecimal applyCoupon(BigDecimal amount, Long couponUserId, Long userId) {
        if (couponUserId == null) return amount;
        CouponUser cu = couponUserMapper.selectById(couponUserId);
        if (cu == null || !cu.getUserId().equals(userId) || cu.getStatus() != 0) {
            throw new RuntimeException("优惠券不可用");
        }
        Coupon c = couponMapper.selectById(cu.getCouponId());
        if (c == null) throw new RuntimeException("优惠券不存在");
        Date now = new Date();
        if ((c.getStartTime() != null && now.before(c.getStartTime())) ||
            (c.getEndTime() != null && now.after(c.getEndTime()))) {
            throw new RuntimeException("优惠券不在有效期内");
        }
        BigDecimal paid = amount;
        if (c.getType() == 1) { // 满减
            if (amount.compareTo(c.getThreshold()) < 0) {
                throw new RuntimeException("未达到满减门槛");
            }
            paid = amount.subtract(c.getValue());
        } else { // 折扣
            paid = amount.multiply(c.getValue());
        }
        if (paid.compareTo(BigDecimal.ZERO) < 0) paid = BigDecimal.ZERO;
        return paid;
    }

    @Override
    public void useCoupon(Long couponUserId, Long orderId) {
        if (couponUserId == null) return;
        couponUserMapper.markUsed(couponUserId, orderId);
    }
}