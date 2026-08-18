package com.parkcar.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户-区域关联（数据权限：收费员负责管理的停车区域）
 */
@Data
@TableName("sys_user_area")
public class SysUserArea {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long areaId;
}
