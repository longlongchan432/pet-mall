package com.petmall.mapper;

import com.petmall.entity.Product;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ProductMapper {
    Product selectById(Long id);
    List<Product> selectList(@Param("name") String name, 
                              @Param("categoryId") Long categoryId,
                              @Param("merchantId") Long merchantId,
                              @Param("status") Integer status);
    List<Product> selectByKeyword(@Param("keyword") String keyword);
    int insert(Product product);
    int update(Product product);
    /** 增减库存；扣库存（quantity 为负）时返回 0 表示库存不足，调用方据此回滚。 */
    int updateStock(@Param("id") Long id, @Param("quantity") Integer quantity);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int delete(Long id);
    long countProducts(@Param("merchantId") Long merchantId);
    List<Product> topProducts(@Param("merchantId") Long merchantId, @Param("limit") int limit);
}

