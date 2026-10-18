package com.petmall.controller;

import com.petmall.common.Result;
import com.petmall.entity.Product;
import com.petmall.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private ProductService productService;

    @GetMapping("/public/list")
    public Result<List<Product>> getProductList(@RequestParam(required = false) String name,
                                                 @RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) Integer status) {
        List<Product> products = productService.getProductList(name, categoryId, null, status);
        return Result.success(products);
    }

    @GetMapping("/public/search")
    public Result<List<Product>> searchProducts(@RequestParam String keyword) {
        List<Product> products = productService.searchProducts(keyword);
        return Result.success(products);
    }

    @GetMapping("/public/{id}")
    public Result<Product> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return Result.success(product);
    }

    // 商家接口
    @GetMapping("/merchant/list")
    public Result<List<Product>> getMerchantProducts(@RequestParam(required = false) String name,
                                                     @RequestParam(required = false) Long categoryId,
                                                     @RequestParam(required = false) Integer status,
                                                     HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 1) {
            return Result.error(403, "无权限访问");
        }
        Long merchantId = (Long) request.getAttribute("userId");
        List<Product> products = productService.getProductList(name, categoryId, merchantId, status);
        return Result.success(products);
    }

    @PostMapping("/merchant/add")
    public Result<String> addProduct(@RequestBody Product product, HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 1) {
            return Result.error(403, "无权限访问");
        }
        Long merchantId = (Long) request.getAttribute("userId");
        product.setMerchantId(merchantId);
        if (productService.addProduct(product)) {
            return Result.success("商品添加成功");
        }
        return Result.error("商品添加失败");
    }

    @PutMapping("/merchant/update")
    public Result<String> updateProduct(@RequestBody Product product, HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 1) {
            return Result.error(403, "无权限访问");
        }
        Long merchantId = (Long) request.getAttribute("userId");
        Product existProduct = productService.getProductById(product.getId());
        if (existProduct == null || !existProduct.getMerchantId().equals(merchantId)) {
            return Result.error("商品不存在或无权限");
        }
        product.setMerchantId(merchantId);
        if (productService.updateProduct(product)) {
            return Result.success("商品更新成功");
        }
        return Result.error("商品更新失败");
    }

    @PutMapping("/merchant/status")
    public Result<String> updateProductStatus(@RequestBody Product product, HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 1) {
            return Result.error(403, "无权限访问");
        }
        Long merchantId = (Long) request.getAttribute("userId");
        Product existProduct = productService.getProductById(product.getId());
        if (existProduct == null || !existProduct.getMerchantId().equals(merchantId)) {
            return Result.error("商品不存在或无权限");
        }
        if (productService.updateProductStatus(product.getId(), product.getStatus())) {
            return Result.success("状态更新成功");
        }
        return Result.error("状态更新失败");
    }

    // 管理员接口
    @GetMapping("/admin/list")
    public Result<List<Product>> getAllProducts(@RequestParam(required = false) String name,
                                                 @RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) Integer status,
                                                 HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 2) {
            return Result.error(403, "无权限访问");
        }
        List<Product> products = productService.getProductList(name, categoryId, null, status);
        return Result.success(products);
    }

    @PutMapping("/admin/status")
    public Result<String> adminUpdateProductStatus(@RequestBody Product product, HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 2) {
            return Result.error(403, "无权限访问");
        }
        if (productService.updateProductStatus(product.getId(), product.getStatus())) {
            return Result.success("状态更新成功");
        }
        return Result.error("状态更新失败");
    }

    @DeleteMapping("/admin/{id}")
    public Result<String> deleteProduct(@PathVariable Long id, HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role != 2) {
            return Result.error(403, "无权限访问");
        }
        if (productService.deleteProduct(id)) {
            return Result.success("商品删除成功");
        }
        return Result.error("商品删除失败");
    }
}



