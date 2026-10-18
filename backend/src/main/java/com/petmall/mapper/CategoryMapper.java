package com.petmall.mapper;

import com.petmall.entity.Category;
import java.util.List;

public interface CategoryMapper {
    Category selectById(Long id);
    List<Category> selectAll();
    List<Category> selectByParentId(Long parentId);
    int insert(Category category);
    int update(Category category);
    int delete(Long id);
}



