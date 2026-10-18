package com.petmall.controller;

import com.petmall.common.Result;
import com.petmall.mapper.OrderMapper;
import com.petmall.mapper.ProductMapper;
import com.petmall.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stats")
public class StatsController {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private OrderMapper orderMapper;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardStats(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        Long userId = (Long) request.getAttribute("userId");
        
        Map<String, Object> stats = new HashMap<>();
        
        if (role == 2) {
            // 管理员统计
            stats.put("totalUsers", userMapper.countUsers(null));
            stats.put("totalProducts", productMapper.countProducts(null));
            stats.put("totalOrders", orderMapper.countOrders(null));
            BigDecimal totalSales = orderMapper.sumSales(null);
            stats.put("totalSales", totalSales != null ? totalSales : BigDecimal.ZERO);
        } else if (role == 1) {
            // 商家统计
            stats.put("totalProducts", productMapper.countProducts(userId));
            stats.put("totalOrders", orderMapper.countOrders(userId));
            BigDecimal totalSales = orderMapper.sumSales(userId);
            stats.put("totalSales", totalSales != null ? totalSales : BigDecimal.ZERO);
            stats.put("totalUsers", 0L); // 商家不显示用户数
        } else {
            return Result.error(403, "无权限访问");
        }
        
        return Result.success(stats);
    }

    @GetMapping("/merchant/trend")
    public Result<List<Map<String, Object>>> merchantTrend(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "7") int days,
            HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != 1) return Result.error(403, "仅商家可访问");
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(orderMapper.dailySales(userId, days));
    }

    @GetMapping("/merchant/top-products")
    public Result<List<com.petmall.entity.Product>> merchantTopProducts(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != 1) return Result.error(403, "仅商家可访问");
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(productMapper.topProducts(userId, 10));
    }
}



