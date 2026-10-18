package com.petmall.mapper;

import com.petmall.entity.UserAddress;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserAddressMapper {
    List<UserAddress> selectByUserId(Long userId);
    UserAddress selectById(Long id);
    int insert(UserAddress address);
    int update(UserAddress address);
    int delete(Long id);
    int clearDefault(@Param("userId") Long userId);
    int setDefault(@Param("id") Long id);
}
