package com.petmall.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Review {
    private Long id;
    private Long orderId;
    private Long productId;
    private Long userId;
    private String userName;
    private Integer rating; // 1-5星
    private String content;
    private String images;
    private Date createTime;
    private String status; // 新增：0-待审核 1-审核通过 2-审核拒绝
}



