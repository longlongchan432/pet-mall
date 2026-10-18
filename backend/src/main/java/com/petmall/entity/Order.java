package com.petmall.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class Order {
    private Long id;
    private String orderNo;
    private Long userId;
    private String userName;
    private Long merchantId;
    private String merchantName;
    private BigDecimal totalAmount;
    private Integer status; // 0-待支付 1-待发货 2-待收货 3-已完成 4-已取消 5-退款中 6-已退款
    private String address;
    private String receiverName;
    private String receiverPhone;
    private String logisticsNo; // 物流单号
    private String logisticsCompany; // 物流公司
    private Date createTime;
    private Date payTime;
    private Date shipTime;
    private Date receiveTime;
    private Date updateTime;
    private List<OrderItem> orderItems;
}



