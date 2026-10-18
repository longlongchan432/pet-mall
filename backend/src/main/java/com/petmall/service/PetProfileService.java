package com.petmall.service;

import com.petmall.entity.PetProfile;
import java.util.List;

public interface PetProfileService {
    List<PetProfile> getPetProfiles(Long userId);
    boolean addPetProfile(PetProfile profile);
    boolean updatePetProfile(PetProfile profile);
    boolean deletePetProfile(Long id);
}
