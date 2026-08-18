package com.parkcar.module.space.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.parkcar.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 车位
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("parking_space")
public class ParkingSpace extends BaseEntity {

    private Long areaId;
    private String spaceNo;
    /** 0=普通 1=充电 2=无障碍 3=VIP */
    private Integer type;
    /** 0=空闲 1=占用 2=禁用 3=维护 */
    private Integer status;
    private String remark;
}
