package com.petmall.mapper;

import com.petmall.entity.Order;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface OrderMapper {
    Order selectById(Long id);
    Order selectByOrderNo(String orderNo);
    List<Order> selectByUserId(Long userId);
    List<Order> selectByMerchantId(Long merchantId);
    List<Order> selectAll(@Param("status") Integer status);
    int insert(Order order);
    int update(Order order);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int updateLogistics(@Param("id") Long id, 
                       @Param("logisticsNo") String logisticsNo,
                       @Param("logisticsCompany") String logisticsCompany);
    long countOrders(@Param("merchantId") Long merchantId);
    BigDecimal sumSales(@Param("merchantId") Long merchantId);
    List<Map<String, Object>> dailySales(@Param("merchantId") Long merchantId, @Param("days") int days);

    // —— 状态机并发保护：条件更新，返回 0 表示状态已被别的请求改掉 ——
    int payIfUnpaid(@Param("id") Long id, @Param("payTime") Date payTime);
    int cancelIfUnpaid(@Param("id") Long id);
    int cancelIfNotFinished(@Param("id") Long id);
    /** 超时未支付的订单 id（供定时任务扫描）。 */
    List<Long> selectExpiredUnpaidIds(@Param("deadline") Date deadline);
}

