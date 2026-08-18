package com.parkcar.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkcar.module.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
