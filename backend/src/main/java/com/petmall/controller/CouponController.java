package com.petmall.controller;

import com.petmall.common.Result;
import com.petmall.entity.Coupon;
import com.petmall.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/coupon")
public class CouponController {
    @Autowired
    private CouponService couponService;

    @GetMapping("/list")
    public Result<List<Coupon>> list(HttpServletRequest request) {
        return Result.success(couponService.getAvailableCoupons());
    }

    @GetMapping("/my")
    public Result<List<Map<String, Object>>> my(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(couponService.getMyCoupons(userId));
    }

    @PostMapping("/receive/{couponId}")
    public Result<String> receive(@PathVariable Long couponId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            if (couponService.receiveCoupon(couponId, userId)) {
                return Result.success("领取成功");
            }
            return Result.error("领取失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 管理员：新建券
    @PostMapping("/add")
    public Result<String> add(@RequestBody Coupon coupon, HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 2) {
            return Result.error(403, "无权限访问");
        }
        coupon.setId(null);
        coupon.setCreateTime(new java.util.Date());
        coupon.setUpdateTime(new java.util.Date());
        // 默认填充
        if (coupon.getStatus() == null) coupon.setStatus(1);
        if (couponService.addCoupon(coupon)) {
            return Result.success("新增成功");
        }
        return Result.error("新增失败");
    }
}