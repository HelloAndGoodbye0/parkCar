package com.parkcar.module.parking.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.parkcar.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 停车记录（在场/历史）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("parking_record")
public class ParkingRecord extends BaseEntity {

    private String plateNo;
    private Long spaceId;
    private Long areaId;
    private LocalDateTime inTime;
    private LocalDateTime outTime;
    /** 0=在场 1=已离场 2=异常 */
    private Integer status;
    /** 是否月卡车辆 */
    private Integer isMember;
    private Long cardId;
    private Long billingRuleId;
    private BigDecimal chargeAmount;
    private BigDecimal paidAmount;
    private BigDecimal discountAmount;
    private Long operatorIn;
    private Long operatorOut;
    private String remark;
}
