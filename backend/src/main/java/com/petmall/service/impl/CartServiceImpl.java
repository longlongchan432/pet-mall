package com.petmall.service.impl;

import com.petmall.entity.Cart;
import com.petmall.mapper.CartMapper;
import com.petmall.mapper.ProductMapper;
import com.petmall.entity.Product;
import com.petmall.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;

    @Override
    public List<Cart> getCartList(Long userId) {
        return cartMapper.selectByUserId(userId);
    }

    @Override
    public boolean addToCart(Cart cart) {
        Cart existCart = cartMapper.selectByUserIdAndProductId(cart.getUserId(), cart.getProductId());
        if (existCart != null) {
            existCart.setQuantity(existCart.getQuantity() + cart.getQuantity());
            existCart.setUpdateTime(new Date());
            return cartMapper.update(existCart) > 0;
        }
        Product product = productMapper.selectById(cart.getProductId());
        if (product == null) {
            return false;
        }
        cart.setProductName(product.getName());
        cart.setProductImage(product.getImage());
        cart.setPrice(product.getPrice());
        cart.setMerchantId(product.getMerchantId());
        cart.setMerchantName(product.getMerchantName());
        cart.setCreateTime(new Date());
        return cartMapper.insert(cart) > 0;
    }

    @Override
    public boolean updateCart(Cart cart) {
        cart.setUpdateTime(new Date());
        return cartMapper.update(cart) > 0;
    }

    @Override
    public boolean deleteCart(Long id) {
        return cartMapper.delete(id) > 0;
    }

    @Override
    public boolean clearCart(Long userId) {
        return cartMapper.deleteByUserId(userId) > 0;
    }
}

