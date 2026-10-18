package com.petmall.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PetProfile {
    private Long id;
    private Long userId;
    private String petType;        // dog/cat/bird/fish/other
    private String breed;          // 品种
    private Integer ageMonths;     // 年龄（月）
    private BigDecimal weightKg;   // 体重（kg）
    private String notes;          // 备注
    private Integer isPrimary;     // 0-否 1-是
    private Date createTime;
    private Date updateTime;
}
