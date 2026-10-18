package com.petmall.service.impl;

import com.petmall.entity.RechargeRecord;
import com.petmall.entity.User;
import com.petmall.mapper.RechargeRecordMapper;
import com.petmall.mapper.UserMapper;
import com.petmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RechargeRecordMapper rechargeRecordMapper;

    // 生成唯一充值单号（新增工具方法）
    private String generateRechargeNo() {
        return "RC" + System.currentTimeMillis() +
                java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }


    @Override
    public User login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return null;
        }
        String hashedPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        if (hashedPassword.equals(user.getPassword())) {
            return user;
        }
        return null;
    }

    @Override
    public boolean register(User user) {
        User existUser = userMapper.selectByUsername(user.getUsername());
        if (existUser != null) {
            return false;
        }
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        // 如果角色未设置，则默认为普通用户
        if (user.getRole() == null) {
            user.setRole(0); // 默认普通用户
        }
        // 如果状态未设置，则默认为启用
        if (user.getStatus() == null) {
            user.setStatus(1); // 默认启用
        }
        user.setCreateTime(new java.util.Date());
        return userMapper.insert(user) > 0;
    }

    @Override
    public User getUserInfo(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public boolean updateUserInfo(User user) {
        user.setUpdateTime(new java.util.Date());
        return userMapper.update(user) > 0;
    }

    @Override
    public boolean updatePassword(Long id, String oldPassword, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return false;
        }
        String hashedOldPassword = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        if (!hashedOldPassword.equals(user.getPassword())) {
            return false;
        }
        String hashedNewPassword = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        return userMapper.updatePassword(id, hashedNewPassword) > 0;
    }

    @Override
    public boolean resetPassword(String email, String newPassword) {
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            return false;
        }
        String hashedPassword = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        return userMapper.updatePassword(user.getId(), hashedPassword) > 0;
    }

    @Override
    public List<User> getUserList(Integer role, Integer status) {
        return userMapper.selectAll(role, status);
    }

    @Override
    public boolean updateUserStatus(Long id, Integer status) {
        return userMapper.updateStatus(id, status) > 0;
    }

    @Override
    public boolean resetUserPassword(Long id, String newPassword) {
        String hashedPassword = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        return userMapper.updatePassword(id, hashedPassword) > 0;
    }

    @Override
    @Transactional
    //修改原有recharge方法
    public boolean recharge(Long userId, BigDecimal amount, Integer paymentMethod) {
        // 原逻辑：直接更新余额 → 新逻辑：仅创建待审核记录
        RechargeRecord record = new RechargeRecord();
        record.setUserId(userId);
        record.setRechargeNo(generateRechargeNo());
        record.setAmount(amount);
        record.setPaymentMethod(paymentMethod);
        record.setStatus(0); // 0-待审核（不再直接设为1）
        record.setCreateTime(new java.util.Date());
        // 仅插入记录，不更新余额
        return rechargeRecordMapper.insert(record) > 0;
    }

    // 新增：更新用户余额（累加）
    @Override
    public boolean updateUserBalance(Long userId, BigDecimal amount) {
        // 1. 查询用户
        User user = getUserInfo(userId);
        if (user == null) {
            return false;
        }
        // 2. 初始化余额（避免null）
        if (user.getBalance() == null) {
            user.setBalance(BigDecimal.ZERO);
        }
        // 3. 累加余额
        user.setBalance(user.getBalance().add(amount));
        // 4. 调用现有方法更新
        return updateUserInfo(user);
    }
}



