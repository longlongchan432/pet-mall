package com.petmall.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Cart {
    private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;
    private Long merchantId;
    private String merchantName;
    private Date createTime;
    private Date updateTime;
}

