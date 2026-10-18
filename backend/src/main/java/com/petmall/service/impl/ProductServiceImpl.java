package com.petmall.service.impl;

import com.petmall.entity.Product;
import com.petmall.mapper.ProductMapper;
import com.petmall.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductMapper productMapper;

    @Override
    public List<Product> getProductList(String name, Long categoryId, Long merchantId, Integer status) {
        return productMapper.selectList(name, categoryId, merchantId, status);
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        return productMapper.selectByKeyword(keyword);
    }

    @Override
    public Product getProductById(Long id) {
        return productMapper.selectById(id);
    }

    @Override
    public boolean addProduct(Product product) {
        product.setStatus(1); // 默认上架
        product.setSales(0);
        product.setCreateTime(new Date());
        return productMapper.insert(product) > 0;
    }

    @Override
    public boolean updateProduct(Product product) {
        product.setUpdateTime(new Date());
        return productMapper.update(product) > 0;
    }

    @Override
    public boolean updateProductStatus(Long id, Integer status) {
        return productMapper.updateStatus(id, status) > 0;
    }

    @Override
    public boolean deleteProduct(Long id) {
        return productMapper.delete(id) > 0;
    }
}



