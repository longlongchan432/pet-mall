package com.petmall.service.impl;

import com.petmall.entity.PetProfile;
import com.petmall.mapper.PetProfileMapper;
import com.petmall.service.PetProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PetProfileServiceImpl implements PetProfileService {
    @Autowired
    private PetProfileMapper petProfileMapper;

    @Override
    public List<PetProfile> getPetProfiles(Long userId) {
        return petProfileMapper.selectByUserId(userId);
    }

    @Override
    public boolean addPetProfile(PetProfile profile) {
        profile.setCreateTime(new Date());
        profile.setUpdateTime(new Date());
        return petProfileMapper.insert(profile) > 0;
    }

    @Override
    public boolean updatePetProfile(PetProfile profile) {
        profile.setUpdateTime(new Date());
        return petProfileMapper.update(profile) > 0;
    }

    @Override
    public boolean deletePetProfile(Long id) {
        return petProfileMapper.delete(id) > 0;
    }
}
