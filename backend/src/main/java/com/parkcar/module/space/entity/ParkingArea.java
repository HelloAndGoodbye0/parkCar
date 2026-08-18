package com.parkcar.module.space.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.parkcar.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 停车区域
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("parking_area")
public class ParkingArea extends BaseEntity {

    private String name;
    private String location;
    /** 车位数量(冗余) */
    private Integer spaceCount;
    private Integer sort;
    /** 1=启用 0=停用 */
    private Integer status;
}
