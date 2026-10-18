package com.petmall.mapper;

import com.petmall.entity.User;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface UserMapper {
    User selectById(Long id);
    User selectByUsername(String username);
    User selectByEmail(String email);
    User selectByPhone(String phone);
    List<User> selectAll(@Param("role") Integer role, @Param("status") Integer status);
    int insert(User user);
    int update(User user);
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int updateBalance(@Param("id") Long id, @Param("amount") java.math.BigDecimal amount);
    long countUsers(@Param("role") Integer role);
}

