package com.parkcar.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkcar.common.BizException;
import com.parkcar.common.PageResult;
import com.parkcar.module.user.entity.SysRole;
import com.parkcar.module.user.entity.SysUser;
import com.parkcar.module.user.entity.SysUserArea;
import com.parkcar.module.user.entity.SysUserRole;
import com.parkcar.module.user.mapper.SysRoleMapper;
import com.parkcar.module.user.mapper.SysUserAreaMapper;
import com.parkcar.module.user.mapper.SysUserMapper;
import com.parkcar.module.user.mapper.SysUserRoleMapper;
import com.parkcar.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理服务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserAreaMapper userAreaMapper;
    private final OperationLogService logService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PageResult<Map<String, Object>> page(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getRealName, keyword))
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getId);
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> records = p.getRecords().stream().map(u -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("phone", u.getPhone());
            m.put("status", u.getStatus());
            m.put("createTime", u.getCreateTime());
            m.put("roles", rolesOf(u.getId()));
            m.put("areaIds", areaIdsOf(u.getId()));
            return m;
        }).collect(Collectors.toList());
        return PageResult.of(p.getTotal(), page, size, records);
    }

    public List<SysRole> roles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    @Transactional
    public void create(SysUser user, List<Long> roleIds, List<Long> areaIds) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));
        if (exists != null && exists > 0) {
            throw BizException.conflict("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);
        saveRoles(user.getId(), roleIds);
        saveAreas(user.getId(), areaIds);
        logService.save("系统管理", "新增用户", "新增用户[" + user.getUsername() + "]");
    }

    @Transactional
    public void update(Long id, SysUser user, List<Long> roleIds, List<Long> areaIds) {
        SysUser exist = userMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setRealName(user.getRealName());
        update.setPhone(user.getPhone());
        userMapper.updateById(update);
        // 重建角色
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        saveRoles(id, roleIds);
        // 重建负责区域
        userAreaMapper.delete(new LambdaQueryWrapper<SysUserArea>().eq(SysUserArea::getUserId, id));
        saveAreas(id, areaIds);
        logService.save("系统管理", "修改用户", "修改用户[" + exist.getUsername() + "]");
    }

    public void changeStatus(Long id, Integer status) {
        if (UserContext.userId() != null && UserContext.userId().equals(id)) {
            throw BizException.badRequest("不能禁用当前登录账号");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
        logService.save("系统管理", "启用/禁用", "调整用户[" + id + "]状态为" + (status == 1 ? "启用" : "禁用"));
    }

    public void resetPassword(Long id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(update);
        logService.save("系统管理", "重置密码", "重置用户[" + user.getUsername() + "]密码");
    }

    @Transactional
    public void delete(Long id) {
        if (UserContext.userId() != null && UserContext.userId().equals(id)) {
            throw BizException.badRequest("不能删除当前登录账号");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            return;
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        userAreaMapper.delete(new LambdaQueryWrapper<SysUserArea>().eq(SysUserArea::getUserId, id));
        logService.save("系统管理", "删除用户", "删除用户[" + user.getUsername() + "]");
    }

    public Set<String> rolesOf(Long userId) {
        List<SysUserRole> relations = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (relations.isEmpty()) {
            return new java.util.LinkedHashSet<>();
        }
        Set<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getCode)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private void saveRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    /** 保存用户负责区域（收费员数据权限） */
    private void saveAreas(Long userId, List<Long> areaIds) {
        if (areaIds == null) {
            return;
        }
        for (Long areaId : areaIds) {
            SysUserArea relation = new SysUserArea();
            relation.setUserId(userId);
            relation.setAreaId(areaId);
            userAreaMapper.insert(relation);
        }
    }

    /** 查询用户负责区域 ID 列表 */
    public List<Long> areaIdsOf(Long userId) {
        return userAreaMapper.selectList(new LambdaQueryWrapper<SysUserArea>()
                        .eq(SysUserArea::getUserId, userId))
                .stream().map(SysUserArea::getAreaId).collect(Collectors.toList());
    }
}
