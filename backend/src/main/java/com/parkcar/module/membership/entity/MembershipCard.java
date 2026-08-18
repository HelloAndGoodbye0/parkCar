package com.parkcar.module.membership.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.parkcar.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 月卡（车辆会员）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("membership_card")
public class MembershipCard extends BaseEntity {

    private String plateNo;
    private Long packageId;
    private String ownerName;
    private String ownerPhone;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 1=有效 0=过期 2=已退订 */
    private Integer status;
}
