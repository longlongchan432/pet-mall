package com.petmall.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Product {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String image;
    private Long categoryId;
    private String categoryName;
    private Long merchantId;
    private String merchantName;
    private Integer status; // 0-下架 1-上架
    private Integer sales; // 销量
    private Date createTime;
    private Date updateTime;
}



