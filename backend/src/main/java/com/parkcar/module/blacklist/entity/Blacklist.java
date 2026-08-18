package com.parkcar.module.blacklist.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.parkcar.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 黑名单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blacklist")
public class Blacklist extends BaseEntity {

    private String plateNo;
    private String reason;
    /** 1=生效 0=解除 */
    private Integer status;
    private Long createBy;
}
