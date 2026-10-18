package com.petmall.controller;

import com.petmall.common.JwtUtil;
import com.petmall.common.Result;
import com.petmall.entity.RechargeRecord;
import com.petmall.entity.User;
import com.petmall.mapper.RechargeRecordMapper;
import com.petmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    // 注入RechargeRecordMapper
    @Autowired
    private RechargeRecordMapper rechargeRecordMapper;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        User user = userService.login(username, password);
        if (user == null) {
            return Result.error("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            return Result.error("账号已被禁用");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return Result.success(data);
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        if (userService.register(user)) {
            return Result.success("注册成功");
        }
        return Result.error("注册失败，用户名已存在");
    }

    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserInfo(userId);
        return Result.success(user);
    }

    @PutMapping("/info")
    public Result<String> updateUserInfo(@RequestBody User user, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        user.setId(userId);
        if (userService.updateUserInfo(user)) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    @PutMapping("/password")
    public Result<String> updatePassword(@RequestBody Map<String, String> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        if (userService.updatePassword(userId, oldPassword, newPassword)) {
            return Result.success("密码修改成功");
        }
        return Result.error("密码修改失败，原密码错误");
    }

    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        String newPassword = params.get("newPassword");
        if (userService.resetPassword(email, newPassword)) {
            return Result.success("密码重置成功");
        }
        return Result.error("密码重置失败，邮箱不存在");
    }

    // 管理员接口
    @GetMapping("/list")
    public Result<List<User>> getUserList(@RequestParam(required = false) Integer role,
                                          @RequestParam(required = false) Integer status,
                                          HttpServletRequest request) {
        Integer userRole = (Integer) request.getAttribute("role");
        if (userRole != 2) {
            return Result.error(403, "无权限访问");
        }
        List<User> users = userService.getUserList(role, status);
        return Result.success(users);
    }

    @PutMapping("/status")
    public Result<String> updateUserStatus(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Integer userRole = (Integer) request.getAttribute("role");
        if (userRole != 2) {
            return Result.error(403, "无权限访问");
        }
        Long id = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        if (userService.updateUserStatus(id, status)) {
            return Result.success("状态更新成功");
        }
        return Result.error("状态更新失败");
    }

    @PutMapping("/reset-password-admin")
    public Result<String> resetUserPassword(@RequestBody Map<String, String> params, HttpServletRequest request) {
        Integer userRole = (Integer) request.getAttribute("role");
        if (userRole != 2) {
            return Result.error(403, "无权限访问");
        }
        Long id = Long.valueOf(params.get("id"));
        String newPassword = params.get("newPassword");
        if (userService.resetUserPassword(id, newPassword)) {
            return Result.success("密码重置成功");
        }
        return Result.error("密码重置失败");
    }

    @PostMapping("/admin/create-merchant")
    public Result<String> createMerchant(@RequestBody User user, HttpServletRequest request) {
        Integer userRole = (Integer) request.getAttribute("role");
        if (userRole != 2) {
            return Result.error(403, "无权限访问");
        }
        user.setRole(1); // 设置为商家角色
        if (userService.register(user)) {
            return Result.success("商家创建成功");
        }
        return Result.error("商家创建失败，用户名已存在");
    }

    /** 账户充值 */
    @PostMapping("/recharge")
    public Result<String> recharge(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        Integer paymentMethod = params.get("paymentMethod") != null ? Integer.valueOf(params.get("paymentMethod").toString()) : 1;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("充值金额必须大于0");
        }
        if (userService.recharge(userId, amount, paymentMethod)) {
            return Result.success("充值申请提交成功，等待管理员审核");
        }
        return Result.error("充值失败");
    }
    /**
     * 管理员分页查询充值记录（仅role=2可访问）
     */
    @GetMapping("/admin/recharge/list")
    public Result<Map<String, Object>> getRechargeRecordList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {
        // 权限校验
        Integer userRole = (Integer) request.getAttribute("role");
        if (userRole != 2) {
            return Result.error(403, "无权限访问");
        }
        // 分页计算
        Integer offset = (pageNum - 1) * pageSize;
        // 查询数据
        List<RechargeRecord> records = rechargeRecordMapper.selectByPage(userId, status, offset, pageSize);
        Integer total = rechargeRecordMapper.count(userId, status);
        // 构造返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("list", records);
        data.put("total", total);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        return Result.success(data);
    }
    /**
     * 管理员审核充值记录（仅role=2可访问）
     */
    @PutMapping("/admin/recharge/audit")
    public Result<String> auditRechargeRecord(
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        // 权限校验
        Integer userRole = (Integer) request.getAttribute("role");
        if (userRole != 2) {
            return Result.error(403, "无权限访问");
        }
        // 解析参数
        Long recordId = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString()); // 1-通过 2-拒绝
        Long auditUserId = (Long) request.getAttribute("userId"); // 审核人ID（可选）

        // 1. 查询记录是否存在且为待审核
        RechargeRecord record = rechargeRecordMapper.selectById(recordId);
        if (record == null || record.getStatus() != 0) {
            return Result.error("记录不存在或非待审核状态");
        }
        // 2. 更新审核状态
        int updateResult = rechargeRecordMapper.updateAuditStatus(recordId, status);
        if (updateResult <= 0) {
            return Result.error("审核失败");
        }
        // 3. 审核通过则更新余额
        if (status == 1) {
            boolean balanceResult = userService.updateUserBalance(record.getUserId(), record.getAmount());
            if (!balanceResult) {
                return Result.error("审核通过，但余额更新失败");
            }
        }
        // 4. 返回结果
        String tip = status == 1 ? "通过" : "拒绝";
        return Result.success("充值记录审核" + tip + "成功");
    }
}

