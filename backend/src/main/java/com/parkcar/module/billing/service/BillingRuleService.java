package com.parkcar.module.billing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.parkcar.common.BizException;
import com.parkcar.module.billing.entity.BillingRule;
import com.parkcar.module.billing.mapper.BillingRuleMapper;
import com.parkcar.module.space.entity.ParkingArea;
import com.parkcar.module.space.mapper.ParkingAreaMapper;
import com.parkcar.module.user.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * 收费规则服务
 */
@Service
@RequiredArgsConstructor
public class BillingRuleService {

    private final BillingRuleMapper ruleMapper;
    private final ParkingAreaMapper areaMapper;
    private final OperationLogService logService;
    private final BillingService billingService;

    public List<BillingRule> list() {
        return ruleMapper.selectList(new LambdaQueryWrapper<BillingRule>()
                .orderByAsc(BillingRule::getId));
    }

    public BillingRule active(Long areaId) {
        return billingService.activeRule(areaId);
    }

    @Transactional
    public void create(BillingRule rule) {
        validateNightFee(rule);
        rule.setId(null);
        rule.setIsDefault(0);
        rule.setVersion(1);
        ruleMapper.insert(rule);
        logService.save("收费管理", "新增规则", "新增收费规则[" + rule.getName() + "]");
    }

    @Transactional
    public void setDefault(Long id) {
        BillingRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw BizException.notFound("规则不存在");
        }
        // 清除其他默认标记，保证全局至多一条
        ruleMapper.update(null, new LambdaUpdateWrapper<BillingRule>()
                .eq(BillingRule::getIsDefault, 1)
                .set(BillingRule::getIsDefault, 0));
        BillingRule update = new BillingRule();
        update.setId(id);
        update.setIsDefault(1);
        ruleMapper.updateById(update);
        logService.save("收费管理", "设置默认规则", "收费规则[" + rule.getName() + "]设为全局默认");
    }

    @Transactional
    public void update(Long id, BillingRule rule) {
        validateNightFee(rule);
        BillingRule exist = ruleMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("规则不存在");
        }
        // 原地更新（version 自增仅作版本记录），区域引用永远有效，历史订单金额已在出场时固化不受影响
        BillingRule update = new BillingRule();
        update.setId(id);
        update.setName(rule.getName());
        update.setRuleType(rule.getRuleType());
        update.setFreeMinutes(rule.getFreeMinutes());
        update.setFirstHourFee(rule.getFirstHourFee());
        update.setHourlyFee(rule.getHourlyFee());
        update.setMaxDailyFee(rule.getMaxDailyFee());
        update.setNightStart(rule.getNightStart());
        update.setNightEnd(rule.getNightEnd());
        update.setNightFee(rule.getNightFee());
        update.setRemark(rule.getRemark());
        update.setVersion(exist.getVersion() + 1);
        ruleMapper.updateById(update);
        logService.save("收费管理", "修改规则", "修改收费规则[" + exist.getName() + "]");
    }

    @Transactional
    public void delete(Long id) {
        BillingRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            return;
        }
        if (rule.getIsDefault() != null && rule.getIsDefault() == 1) {
            throw BizException.conflict("全局默认规则不能删除，请先设置其他默认规则");
        }
        Long count = areaMapper.selectCount(new LambdaQueryWrapper<ParkingArea>()
                .eq(ParkingArea::getBillingRuleId, id));
        if (count != null && count > 0) {
            throw BizException.conflict("该规则已被区域绑定，请先在区域管理中解除绑定");
        }
        ruleMapper.deleteById(id);
        logService.save("收费管理", "删除规则", "删除收费规则[" + rule.getName() + "]");
    }

    /**
     * 校验夜间计费配置：开始、结束、费用必须同空或同非空；按次计费不支持夜间计费。
     */
    private void validateNightFee(BillingRule rule) {
        LocalTime start = rule.getNightStart();
        LocalTime end = rule.getNightEnd();
        BigDecimal fee = rule.getNightFee();
        boolean hasNight = start != null || end != null || fee != null;
        if (hasNight && (start == null || end == null || fee == null)) {
            throw BizException.badRequest("夜间计费需同时设置开始时间、结束时间和夜间费用，或全部留空");
        }
        if (start != null) {
            if (start.equals(end)) {
                throw BizException.badRequest("夜间计费的开始与结束时间不能相同");
            }
            if (fee.compareTo(BigDecimal.ZERO) < 0) {
                throw BizException.badRequest("夜间费用不能为负数");
            }
            if (rule.getRuleType() != null && rule.getRuleType() == 1) {
                throw BizException.badRequest("按次计费规则不支持夜间计费");
            }
        }
    }
}
