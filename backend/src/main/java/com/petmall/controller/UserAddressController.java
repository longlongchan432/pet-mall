package com.petmall.controller;

import com.petmall.common.Result;
import com.petmall.entity.UserAddress;
import com.petmall.service.UserAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/address")
public class UserAddressController {
    @Autowired
    private UserAddressService userAddressService;

    @GetMapping("/list")
    public Result<List<UserAddress>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userAddressService.list(userId));
    }

    @PostMapping("/add")
    public Result<String> add(@RequestBody UserAddress addr, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (addr.getReceiverName() == null || addr.getReceiverPhone() == null || addr.getDetail() == null) {
            return Result.error("收货信息不完整");
        }
        return userAddressService.add(userId, addr) ? Result.success("添加成功") : Result.error("添加失败");
    }

    @PutMapping("/update/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody UserAddress addr, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        addr.setId(id);
        return userAddressService.update(userId, addr) ? Result.success("更新成功") : Result.error("地址不存在或无权限");
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userAddressService.delete(userId, id) ? Result.success("删除成功") : Result.error("地址不存在或无权限");
    }

    @PostMapping("/default/{id}")
    public Result<String> setDefault(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userAddressService.setDefault(userId, id) ? Result.success("已设为默认") : Result.error("地址不存在或无权限");
    }
}
