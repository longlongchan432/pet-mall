package com.petmall.controller;

import com.petmall.common.Result;
import com.petmall.entity.Cart;
import com.petmall.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {
    @Autowired
    private CartService cartService;

    @GetMapping("/list")
    public Result<List<Cart>> getCartList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Cart> carts = cartService.getCartList(userId);
        return Result.success(carts);
    }

    @PostMapping("/add")
    public Result<String> addToCart(@RequestBody Cart cart, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        cart.setUserId(userId);
        if (cartService.addToCart(cart)) {
            return Result.success("添加成功");
        }
        return Result.error("添加失败");
    }

    @PutMapping("/update")
    public Result<String> updateCart(@RequestBody Cart cart, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        cart.setUserId(userId);
        if (cartService.updateCart(cart)) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteCart(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Cart cart = cartService.getCartList(userId).stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (cart == null) {
            return Result.error("购物车项不存在");
        }
        if (cartService.deleteCart(id)) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }

    @DeleteMapping("/clear")
    public Result<String> clearCart(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (cartService.clearCart(userId)) {
            return Result.success("清空成功");
        }
        return Result.error("清空失败");
    }
}



