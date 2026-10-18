package com.petmall.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Coupon {
    private Long id;
    private String name;        // 名称
    private Integer type;       // 1-满减 2-折扣
    private BigDecimal threshold; // 满X元可用
    private BigDecimal value;   // 减X元 / 折X（如0.9）
    private Integer stock;      // 剩余库存
    private Integer total;      // 发行总量
    private Integer status;     // 1-正常 0-下架
    private Date startTime;
    private Date endTime;
    private Date createTime;
    private Date updateTime;
}