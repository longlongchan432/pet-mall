package com.petmall.entity;

import lombok.Data;
import java.util.Date;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String realName;
    private String address;
    private java.math.BigDecimal balance; // 账户余额
    private Integer role; // 0-普通用户 1-商家 2-管理员
    private Integer status; // 0-禁用 1-启用
    private Date createTime;
    private Date updateTime;
}



