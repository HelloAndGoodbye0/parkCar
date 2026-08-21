package com.parkcar.module.billing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 计费明细：总金额 + 逐项明细（按天、按夜、按次等）
 */
@Data
public class BillingDetail {

    /** 应收总额 */
    private BigDecimal total;

    /** 明细项 */
    private List<Item> items;

    @Data
    public static class Item {
        /** 类型：DAY 白天 / NIGHT 夜间 / ONCE 按次 / FREE 免费 */
        private String type;
        /** 时段，如 "08-18 15:02 ~ 08-19 00:00" */
        private String period;
        /** 说明，如 "8小时42分钟" / "夜间 22:00~06:00" / "已达每日封顶" */
        private String desc;
        /** 金额 */
        private BigDecimal amount;
    }
}
