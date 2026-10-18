package com.petmall.mapper;

import com.petmall.entity.Review;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ReviewMapper {
    Review selectById(Long id);
    List<Review> selectByProductId(Long productId);
    List<Review> selectByOrderId(Long orderId);
    List<Review> selectByUserId(Long userId);
    int insert(Review review);
    int update(Review review);
    int delete(Long id);

    // 新增管理员相关方法
    // 分页查评价（status：0待审核/1通过/2拒绝）
    List<Review> selectByPage(
            @Param("status") Integer status,
            @Param("productId") Long productId,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );
    // 统计评价总数
    Integer count(
            @Param("status") Integer status,
            @Param("productId") Long productId
    );
    // 更新评价状态
    int updateStatus(
            @Param("id") Long id,
            @Param("status") Integer status
    );
}



