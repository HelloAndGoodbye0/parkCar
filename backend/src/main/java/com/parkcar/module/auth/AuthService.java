package com.parkcar.module.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkcar.common.BizException;
import com.parkcar.module.user.entity.SysRole;
import com.parkcar.module.user.entity.SysUser;
import com.parkcar.module.user.entity.SysUserRole;
import com.parkcar.module.user.mapper.SysRoleMapper;
import com.parkcar.module.user.mapper.SysUserMapper;
import com.parkcar.module.user.mapper.SysUserRoleMapper;
import com.parkcar.module.user.service.OperationLogService;
import com.parkcar.security.JwtUtil;
import com.parkcar.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证服务：登录、当前用户信息
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final JwtUtil jwtUtil;
    private final OperationLogService logService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> login(String username, String password) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw BizException.badRequest("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.forbidden("账号已被禁用");
        }
        Set<String> roles = rolesOf(user.getId());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roles);

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("roles", roles);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("user", userInfo);

        logService.save("认证", "登录", "用户[" + username + "]登录系统");
        return data;
    }

    public Map<String, Object> me() {
        Long userId = UserContext.userId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.unauthorized("用户不存在");
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("roles", rolesOf(userId));
        return info;
    }

    /**
     * 查询用户角色编码集合
     */
    public Set<String> rolesOf(Long userId) {
        List<SysUserRole> relations = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (relations.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
