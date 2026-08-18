package com.parkcar.module.billing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.parkcar.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 收费规则
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("billing_rule")
public class BillingRule extends BaseEntity {

    private String name;
    /** 0=按时 1=按次 */
    private Integer ruleType;
    /** 免费时长(分钟) */
    private Integer freeMinutes;
    /** 首小时/单次费用 */
    private BigDecimal firstHourFee;
    /** 之后每小时费用 */
    private BigDecimal hourlyFee;
    /** 每日封顶价 NULL=无封顶 */
    private BigDecimal maxDailyFee;
    private LocalTime nightStart;
    private LocalTime nightEnd;
    private BigDecimal nightFee;
    /** 1=启用 0=停用 */
    private Integer enabled;
    private Integer version;
    private String remark;
}
