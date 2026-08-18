package com.parkcar.module.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkcar.module.billing.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
