package com.petmall.service;

import com.petmall.entity.Cart;
import java.util.List;

public interface CartService {
    List<Cart> getCartList(Long userId);
    boolean addToCart(Cart cart);
    boolean updateCart(Cart cart);
    boolean deleteCart(Long id);
    boolean clearCart(Long userId);
}



