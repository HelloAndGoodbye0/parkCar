package com.parkcar.module.billing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkcar.common.BizException;
import com.parkcar.module.billing.entity.BillingRule;
import com.parkcar.module.billing.mapper.BillingRuleMapper;
import com.parkcar.module.parking.entity.ParkingRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 计费引擎
 */
@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingRuleMapper billingRuleMapper;

    /**
     * 获取当前启用规则
     */
    public BillingRule activeRule() {
        BillingRule rule = billingRuleMapper.selectOne(new LambdaQueryWrapper<BillingRule>()
                .eq(BillingRule::getEnabled, 1)
                .orderByDesc(BillingRule::getId)
                .last("LIMIT 1"));
        if (rule == null) {
            throw BizException.badRequest("未配置启用的收费规则，请先在收费规则中启用一条");
        }
        return rule;
    }

    /**
     * 计算停车费用
     *
     * @param inTime   入场时间
     * @param rule     收费规则
     * @param freeFlag 是否免费（月卡有效期内）
     * @return 应收金额
     */
    public BigDecimal calculate(LocalDateTime inTime, BillingRule rule, boolean freeFlag) {
        if (freeFlag) {
            return BigDecimal.ZERO;
        }
        long minutes = Math.max(0, Duration.between(inTime, LocalDateTime.now()).toMinutes());
        int freeMinutes = rule.getFreeMinutes() == null ? 0 : rule.getFreeMinutes();
        if (minutes <= freeMinutes) {
            return BigDecimal.ZERO;
        }
        BigDecimal fee;
        if (rule.getRuleType() != null && rule.getRuleType() == 1) {
            // 按次
            fee = nvl(rule.getFirstHourFee());
        } else {
            // 按时
            long chargedMinutes = minutes - freeMinutes;
            long hours = (chargedMinutes + 59) / 60; // 向上取整
            if (hours <= 1) {
                fee = nvl(rule.getFirstHourFee());
            } else {
                fee = nvl(rule.getFirstHourFee())
                        .add(nvl(rule.getHourlyFee()).multiply(BigDecimal.valueOf(hours - 1)));
            }
            BigDecimal maxDaily = rule.getMaxDailyFee();
            if (maxDaily != null && fee.compareTo(maxDaily) > 0) {
                fee = maxDaily;
            }
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculate(ParkingRecord record, BillingRule rule) {
        return calculate(record.getInTime(), rule, record.getIsMember() != null && record.getIsMember() == 1);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
