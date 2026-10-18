package com.petmall.controller;

import com.petmall.common.Result;
import com.petmall.entity.Review;
import com.petmall.mapper.ReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/review")
public class ReviewController {
    @Autowired
    private ReviewMapper reviewMapper;

    // 原有接口全部保留（仅修改addReview）
    @GetMapping("/product/{productId}")
    public Result<List<Review>> getProductReviews(@PathVariable Long productId) {
        List<Review> reviews = reviewMapper.selectByProductId(productId);
        return Result.success(reviews);
    }

    @GetMapping("/my-list")
    public Result<List<Review>> getMyReviews(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Review> reviews = reviewMapper.selectByUserId(userId);
        return Result.success(reviews); // 现在返回的review包含status字段，用户可查状态
    }

    // 修改addReview：新增评价默认设为待审核（status=0）
    @PostMapping("/add")
    public Result<String> addReview(@RequestBody Review review, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        review.setUserId(userId);
        review.setCreateTime(new Date());
        review.setStatus(String.valueOf(0)); // 新增：默认待审核
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            return Result.error("请选择1-5星评分");
        }
        if (review.getProductId() == null) {
            return Result.error("请选择要评价的商品");
        }
        if (reviewMapper.insert(review) > 0) {
            return Result.success("评价提交成功，等待管理员审核"); // 修改提示语
        }
        return Result.error("评价失败");
    }

    // ---------------------- 新增管理员接口 ----------------------
    /**
     * 管理员分页查待审核评价
     */
    @GetMapping("/admin/list")
    public Result<Map<String, Object>> getAdminReviewList(
            @RequestParam(defaultValue = "0") Integer status, // 默认查待审核
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request
    ) {
        // 校验管理员权限（role=2）
        Integer userRole = (Integer) request.getAttribute("role");
        if (userRole != 2) {
            return Result.error(403, "无权限访问");
        }
        Integer offset = (pageNum - 1) * pageSize;
        List<Review> reviews = reviewMapper.selectByPage(status, productId, offset, pageSize);
        Integer total = reviewMapper.count(status, productId);

        Map<String, Object> data = new HashMap<>();
        data.put("list", reviews);
        data.put("total", total);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        return Result.success(data);
    }

    /**
     * 管理员审核通过评价（改status=1）
     */
    @PutMapping("/admin/pass/{id}")
    public Result<String> passReview(@PathVariable Long id, HttpServletRequest request) {
        // 权限校验
        Integer userRole = (Integer) request.getAttribute("role");
        if (userRole != 2) {
            return Result.error(403, "无权限操作");
        }
        if (reviewMapper.updateStatus(id, 1) > 0) {
            return Result.success("评价审核通过");
        }
        return Result.error("审核失败");
    }

    /**
     * 管理员拒绝评价（直接删除）
     */
    @DeleteMapping("/admin/reject/{id}")
    public Result<String> rejectReview(@PathVariable Long id, HttpServletRequest request) {
        // 权限校验
        Integer userRole = (Integer) request.getAttribute("role");
        if (userRole != 2) {
            return Result.error(403, "无权限操作");
        }
        if (reviewMapper.delete(id) > 0) {
            return Result.success("评价已拒绝并删除");
        }
        return Result.error("拒绝失败");
    }
}