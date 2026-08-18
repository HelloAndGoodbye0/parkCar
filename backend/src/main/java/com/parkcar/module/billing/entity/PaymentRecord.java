package com.parkcar.module.billing.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水
 */
@Data
@TableName("payment_record")
public class PaymentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    /** 1=现金 2=微信 3=支付宝 */
    private Integer payType;
    private BigDecimal amount;
    private String transactionNo;
    /** 1=成功 0=失败 */
    private Integer status;
    private Long operatorId;
    private LocalDateTime payTime;
}
