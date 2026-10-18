package com.petmall.service;

import com.petmall.entity.UserAddress;
import com.petmall.mapper.UserAddressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class UserAddressService {
    @Autowired
    private UserAddressMapper userAddressMapper;

    public List<UserAddress> list(Long userId) {
        return userAddressMapper.selectByUserId(userId);
    }

    @Transactional
    public boolean add(Long userId, UserAddress addr) {
        addr.setUserId(userId);
        Date now = new Date();
        addr.setCreateTime(now);
        addr.setUpdateTime(now);
        // 第一条地址自动为默认
        if (userAddressMapper.selectByUserId(userId).isEmpty()) {
            addr.setIsDefault(1);
        } else if (Integer.valueOf(1).equals(addr.getIsDefault())) {
            userAddressMapper.clearDefault(userId);
        } else {
            addr.setIsDefault(0);
        }
        return userAddressMapper.insert(addr) > 0;
    }

    @Transactional
    public boolean update(Long userId, UserAddress addr) {
        UserAddress db = userAddressMapper.selectById(addr.getId());
        if (db == null || !db.getUserId().equals(userId)) {
            return false;
        }
        addr.setUserId(userId);
        addr.setUpdateTime(new Date());
        if (Integer.valueOf(1).equals(addr.getIsDefault())) {
            userAddressMapper.clearDefault(userId);
        }
        return userAddressMapper.update(addr) > 0;
    }

    @Transactional
    public boolean delete(Long userId, Long id) {
        UserAddress db = userAddressMapper.selectById(id);
        if (db == null || !db.getUserId().equals(userId)) {
            return false;
        }
        boolean ok = userAddressMapper.delete(id) > 0;
        // 删掉默认地址后，把剩下的第一条顶上来
        if (ok && Integer.valueOf(1).equals(db.getIsDefault())) {
            List<UserAddress> rest = userAddressMapper.selectByUserId(userId);
            if (!rest.isEmpty()) {
                userAddressMapper.setDefault(rest.get(0).getId());
            }
        }
        return ok;
    }

    @Transactional
    public boolean setDefault(Long userId, Long id) {
        UserAddress db = userAddressMapper.selectById(id);
        if (db == null || !db.getUserId().equals(userId)) {
            return false;
        }
        userAddressMapper.clearDefault(userId);
        return userAddressMapper.setDefault(id) > 0;
    }
}
