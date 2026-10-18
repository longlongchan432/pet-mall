package com.petmall.mapper;

import com.petmall.entity.Cart;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CartMapper {
    Cart selectById(Long id);
    Cart selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
    List<Cart> selectByUserId(Long userId);
    int insert(Cart cart);
    int update(Cart cart);
    int delete(Long id);
    int deleteByUserId(Long userId);
}





