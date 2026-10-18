package com.petmall.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Category {
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private Integer sort;
    private Date createTime;
    private Date updateTime;
}



