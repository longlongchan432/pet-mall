package com.petmall.service;

import com.petmall.entity.Product;
import java.util.List;

public interface ProductService {
    List<Product> getProductList(String name, Long categoryId, Long merchantId, Integer status);
    List<Product> searchProducts(String keyword);
    Product getProductById(Long id);
    boolean addProduct(Product product);
    boolean updateProduct(Product product);
    boolean updateProductStatus(Long id, Integer status);
    boolean deleteProduct(Long id);
}



