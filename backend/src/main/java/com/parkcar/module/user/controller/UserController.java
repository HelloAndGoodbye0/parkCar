package com.parkcar.module.user.controller;

import com.parkcar.common.PageResult;
import com.parkcar.common.Result;
import com.parkcar.module.user.entity.SysRole;
import com.parkcar.module.user.entity.SysUser;
import com.parkcar.module.user.service.UserService;
import com.parkcar.security.RequireRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Data
    public static class UserRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        private String password;
        private String realName;
        private String phone;
        private List<Long> roleIds;
        /** 负责区域ID列表（收费员数据权限，管理员可忽略） */
        private List<Long> areaIds;
    }

    @Data
    public static class StatusRequest {
        @NotNull(message = "状态不能为空")
        private Integer status;
    }

    @Data
    public static class PasswordRequest {
        @NotBlank(message = "新密码不能为空")
        private String password;
    }

    @GetMapping
    @RequireRole("ADMIN")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Integer status) {
        return Result.ok(userService.page(page, size, keyword, status));
    }

    @GetMapping("/roles")
    @RequireRole("ADMIN")
    public Result<List<SysRole>> roles() {
        return Result.ok(userService.roles());
    }

    @PostMapping
    @RequireRole("ADMIN")
    public Result<Void> create(@RequestBody UserRequest req) {
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(req.getPassword() == null || req.getPassword().isBlank() ? "123456" : req.getPassword());
        user.setRealName(req.getRealName());
        user.setPhone(req.getPhone());
        userService.create(user, req.getRoleIds(), req.getAreaIds());
        return Result.ok();
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserRequest req) {
        SysUser user = new SysUser();
        user.setRealName(req.getRealName());
        user.setPhone(req.getPhone());
        userService.update(id, user, req.getRoleIds(), req.getAreaIds());
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @RequireRole("ADMIN")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        userService.changeStatus(id, req.getStatus());
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    @RequireRole("ADMIN")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody PasswordRequest req) {
        userService.resetPassword(id, req.getPassword());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
