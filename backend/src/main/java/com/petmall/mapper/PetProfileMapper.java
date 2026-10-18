package com.petmall.mapper;

import com.petmall.entity.PetProfile;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PetProfileMapper {
    List<PetProfile> selectByUserId(Long userId);
    PetProfile selectById(Long id);
    int insert(PetProfile petProfile);
    int update(PetProfile petProfile);
    int delete(Long id);
}
