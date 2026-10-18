package com.petmall.entity;

import lombok.Data;
import java.util.Date;

@Data
public class CouponUser {
    private Long id;
    private Long couponId;
    private Long userId;
    private Integer status;     // 0-未用 1-已用
    private Long orderId;
    private Date receiveTime;
    private Date useTime;
    private Date createTime;
    private Date updateTime;
}