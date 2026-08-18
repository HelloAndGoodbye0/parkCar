package com.parkcar.module.parking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkcar.module.parking.entity.ParkingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 停车记录 Mapper
 */
@Mapper
public interface ParkingRecordMapper extends BaseMapper<ParkingRecord> {

    /**
     * 行锁查询在场记录，防止并发出场
     */
    @Select("SELECT * FROM parking_record WHERE id = #{id} AND status = 0 AND deleted = 0 FOR UPDATE")
    ParkingRecord selectInRecordForUpdate(@Param("id") Long id);
}
