package com.parkcar.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkcar.module.user.entity.SysRole;
import com.parkcar.module.user.entity.SysUser;
import com.parkcar.module.user.entity.SysUserRole;
import com.parkcar.module.user.mapper.SysRoleMapper;
import com.parkcar.module.user.mapper.SysUserMapper;
import com.parkcar.module.user.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化管理员账号（仅当不存在时）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitRunner implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Value("${parkcar.admin.username:admin}")
    private String adminUsername;

    @Value("${parkcar.admin.password:123456}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, adminUsername));
        if (count != null && count > 0) {
            return;
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        SysUser admin = new SysUser();
        admin.setUsername(adminUsername);
        admin.setPassword(encoder.encode(adminPassword));
        admin.setRealName("系统管理员");
        admin.setStatus(1);
        userMapper.insert(admin);

        // 兜底：若 seed.sql 未执行，自动创建角色
        SysRole adminRole = ensureRole("ADMIN", "管理员", "系统全部权限");
        ensureRole("OPERATOR", "收费员", "出入场登记与收费");
        if (adminRole != null) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(admin.getId());
            relation.setRoleId(adminRole.getId());
            userRoleMapper.insert(relation);
        }
        log.info("已初始化管理员账号: {} / {}", adminUsername, adminPassword);
    }

    private SysRole ensureRole(String code, String name, String remark) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, code));
        if (role == null) {
            role = new SysRole();
            role.setCode(code);
            role.setName(name);
            role.setRemark(remark);
            roleMapper.insert(role);
        }
        return role;
    }
}
