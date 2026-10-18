package com.petmall.controller;

import com.petmall.common.Result;
import com.petmall.entity.Category;
import com.petmall.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryMapper categoryMapper;

    @GetMapping("/public/list")
    public Result<List<Category>> getCategoryList() {
        List<Category> categories = categoryMapper.selectAll();
        return Result.success(categories);
    }

    @GetMapping("/public/{id}")
    public Result<Category> getCategoryById(@PathVariable Long id) {
        Category category = categoryMapper.selectById(id);
        return Result.success(category);
    }

    // 管理员接口
    @GetMapping("/admin/list")
    public Result<List<Category>> getAdminCategoryList(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 2) {
            return Result.error(403, "无权限访问");
        }
        List<Category> categories = categoryMapper.selectAll();
        return Result.success(categories);
    }

    @PostMapping("/admin/add")
    public Result<String> addCategory(@RequestBody Category category, HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 2) {
            return Result.error(403, "无权限访问");
        }
        category.setCreateTime(new Date());
        if (categoryMapper.insert(category) > 0) {
            return Result.success("分类添加成功");
        }
        return Result.error("分类添加失败");
    }

    @PutMapping("/admin/update")
    public Result<String> updateCategory(@RequestBody Category category, HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 2) {
            return Result.error(403, "无权限访问");
        }
        category.setUpdateTime(new Date());
        if (categoryMapper.update(category) > 0) {
            return Result.success("分类更新成功");
        }
        return Result.error("分类更新失败");
    }

    @DeleteMapping("/admin/{id}")
    public Result<String> deleteCategory(@PathVariable Long id, HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 2) {
            return Result.error(403, "无权限访问");
        }
        if (categoryMapper.delete(id) > 0) {
            return Result.success("分类删除成功");
        }
        return Result.error("分类删除失败");
    }
}

