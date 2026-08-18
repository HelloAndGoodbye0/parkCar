package com.parkcar.module.membership.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.parkcar.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 会员套餐
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("membership_package")
public class MembershipPackage extends BaseEntity {

    private String name;
    /** 有效期天数 */
    private Integer durationDays;
    private BigDecimal price;
    /** 1=上架 0=下架 */
    private Integer status;
    private String remark;
}
