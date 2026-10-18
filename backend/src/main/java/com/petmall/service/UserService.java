package com.petmall.service;

import com.petmall.entity.User;
import java.util.List;
import java.math.BigDecimal;

public interface UserService {
    User login(String username, String password);
    boolean register(User user);
    User getUserInfo(Long id);
    boolean updateUserInfo(User user);
    boolean updatePassword(Long id, String oldPassword, String newPassword);
    boolean resetPassword(String email, String newPassword);
    List<User> getUserList(Integer role, Integer status);
    boolean updateUserStatus(Long id, Integer status);
    boolean resetUserPassword(Long id, String newPassword);
    boolean recharge(Long userId, java.math.BigDecimal amount, Integer paymentMethod);

    // 新增：更新用户余额（核心方法）
    boolean updateUserBalance(Long userId, BigDecimal amount);
}



