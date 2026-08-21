package com.parkcar.module.membership.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.parkcar.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    /** 活动开始时间，NULL=长期有效 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    /** 活动结束时间，NULL=长期有效 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private String remark;
}
