package com.petmall.controller;

import com.petmall.common.Result;
import com.petmall.entity.PetProfile;
import com.petmall.service.PetProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/pet-profile")
public class PetProfileController {
    @Autowired
    private PetProfileService petProfileService;

    @GetMapping("/list")
    public Result<List<PetProfile>> getPetProfiles(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<PetProfile> profiles = petProfileService.getPetProfiles(userId);
        return Result.success(profiles);
    }

    @PostMapping
    public Result<String> addPetProfile(@RequestBody PetProfile profile, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        profile.setUserId(userId);
        if (petProfileService.addPetProfile(profile)) {
            return Result.success("添加成功");
        }
        return Result.error("添加失败");
    }

    @PutMapping("/{id}")
    public Result<String> updatePetProfile(@PathVariable Long id, @RequestBody PetProfile profile, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        profile.setId(id);
        profile.setUserId(userId);
        if (petProfileService.updatePetProfile(profile)) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<String> deletePetProfile(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        // 严格隔离：只允许操作自己的档案
        PetProfile profile = petProfileService.getPetProfiles(userId).stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (profile == null) {
            return Result.error("宠物档案不存在或无权限");
        }
        if (petProfileService.deletePetProfile(id)) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }
}
