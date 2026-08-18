package com.parkcar.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkcar.common.BizException;
import com.parkcar.module.user.entity.SysUserArea;
import com.parkcar.module.user.mapper.SysUserAreaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 区域数据权限助手
 * <p>管理员可见全部区域（返回 null）；收费员仅可见被分配的负责区域。</p>
 */
@Component
@RequiredArgsConstructor
public class AreaScopeHelper {

    private final SysUserAreaMapper userAreaMapper;

    /**
     * 当前用户可见的区域 ID 集合
     *
     * @return 管理员返回 null（表示全部区域可见）；收费员返回负责区域 ID 列表（可能为空）
     */
    public List<Long> visibleAreaIds() {
        if (UserContext.isAdmin()) {
            return null;
        }
        return userAreaMapper.selectList(new LambdaQueryWrapper<SysUserArea>()
                        .eq(SysUserArea::getUserId, UserContext.userId()))
                .stream().map(SysUserArea::getAreaId).collect(Collectors.toList());
    }

    /**
     * 判断当前用户是否有权访问指定区域（areaId 为 null 时放行）
     */
    public boolean hasAreaAccess(Long areaId) {
        if (areaId == null) {
            return true;
        }
        List<Long> visible = visibleAreaIds();
        return visible == null || visible.contains(areaId);
    }

    /**
     * 校验当前用户是否有权访问指定区域，无权则抛 403
     */
    public void checkAreaAccess(Long areaId, String message) {
        if (!hasAreaAccess(areaId)) {
            throw BizException.forbidden(message);
        }
    }
}
