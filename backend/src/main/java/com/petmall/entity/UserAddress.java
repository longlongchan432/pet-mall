package com.petmall.entity;

import lombok.Data;
import java.util.Date;

@Data
public class UserAddress {
    private Long id;
    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private String detail;      // 详细收货地址
    private Integer isDefault;  // 0-否 1-是
    private Date createTime;
    private Date updateTime;
}
