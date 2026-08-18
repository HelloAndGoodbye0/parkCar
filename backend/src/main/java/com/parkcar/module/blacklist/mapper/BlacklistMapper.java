package com.parkcar.module.blacklist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkcar.module.blacklist.entity.Blacklist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlacklistMapper extends BaseMapper<Blacklist> {
}
