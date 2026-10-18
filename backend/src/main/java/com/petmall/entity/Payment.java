package com.petmall.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Payment {
    private Long id;
    private String paymentNo;
    private Long orderId;
    private String orderNo;
    private BigDecimal amount;
    private Integer paymentMethod; // 1-支付宝 2-微信 3-银行卡
    private Integer status; // 0-未支付 1-已支付 2-已退款
    private Date createTime;
    private Date payTime;
}



