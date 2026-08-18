package com.parkcar.module.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkcar.module.billing.entity.BillingOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BillingOrderMapper extends BaseMapper<BillingOrder> {
}
