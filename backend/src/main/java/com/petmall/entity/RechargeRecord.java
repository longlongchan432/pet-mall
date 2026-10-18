package com.petmall.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class RechargeRecord {
    private Long id;
    private Long userId;
    private String rechargeNo;
    private BigDecimal amount;
    private Integer paymentMethod; // 1-支付宝 2-微信 3-银行卡
    private Integer status; // 0-失败 1-成功
    private Date createTime;
}
