package com.parkcar.module.billing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.parkcar.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收费订单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("billing_order")
public class BillingOrder extends BaseEntity {

    private String orderNo;
    private Long recordId;
    private String plateNo;
    private BigDecimal amount;
    private BigDecimal discount;
    private BigDecimal paidAmount;
    /** 1=现金 2=微信 3=支付宝 4=月卡抵扣 */
    private Integer payType;
    /** 0=待支付 1=已支付 2=已取消 */
    private Integer status;
    private Long operatorId;
    private LocalDateTime payTime;
    private String remark;
}
