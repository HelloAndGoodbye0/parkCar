package com.parkcar.module.billing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.parkcar.common.BizException;
import com.parkcar.module.billing.entity.BillingRule;
import com.parkcar.module.billing.mapper.BillingRuleMapper;
import com.parkcar.module.user.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收费规则服务
 */
@Service
@RequiredArgsConstructor
public class BillingRuleService {

    private final BillingRuleMapper ruleMapper;
    private final OperationLogService logService;

    public List<BillingRule> list() {
        return ruleMapper.selectList(new LambdaQueryWrapper<BillingRule>()
                .orderByDesc(BillingRule::getEnabled)
                .orderByAsc(BillingRule::getId));
    }

    public BillingRule active() {
        BillingRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<BillingRule>()
                .eq(BillingRule::getEnabled, 1)
                .last("LIMIT 1"));
        if (rule == null) {
            throw BizException.notFound("暂无启用的收费规则");
        }
        return rule;
    }

    @Transactional
    public void create(BillingRule rule) {
        rule.setId(null);
        rule.setEnabled(0);
        rule.setVersion(1);
        ruleMapper.insert(rule);
        logService.save("收费管理", "新增规则", "新增收费规则[" + rule.getName() + "]");
    }

    @Transactional
    public void enable(Long id) {
        BillingRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw BizException.notFound("规则不存在");
        }
        // 停用其他规则
        ruleMapper.update(null, new LambdaUpdateWrapper<BillingRule>()
                .eq(BillingRule::getEnabled, 1)
                .set(BillingRule::getEnabled, 0));
        BillingRule update = new BillingRule();
        update.setId(id);
        update.setEnabled(1);
        ruleMapper.updateById(update);
        logService.save("收费管理", "启用规则", "启用收费规则[" + rule.getName() + "]");
    }

    @Transactional
    public void update(Long id, BillingRule rule) {
        BillingRule exist = ruleMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("规则不存在");
        }
        // 生成新版本
        BillingRule newVersion = new BillingRule();
        newVersion.setName(rule.getName());
        newVersion.setRuleType(rule.getRuleType());
        newVersion.setFreeMinutes(rule.getFreeMinutes());
        newVersion.setFirstHourFee(rule.getFirstHourFee());
        newVersion.setHourlyFee(rule.getHourlyFee());
        newVersion.setMaxDailyFee(rule.getMaxDailyFee());
        newVersion.setNightStart(rule.getNightStart());
        newVersion.setNightEnd(rule.getNightEnd());
        newVersion.setNightFee(rule.getNightFee());
        newVersion.setEnabled(exist.getEnabled());
        newVersion.setVersion(exist.getVersion() + 1);
        newVersion.setRemark(rule.getRemark());
        ruleMapper.insert(newVersion);

        // 原规则停用保留历史
        BillingRule disable = new BillingRule();
        disable.setId(id);
        disable.setEnabled(0);
        ruleMapper.updateById(disable);
        logService.save("收费管理", "修改规则", "修改收费规则[" + exist.getName() + "]生成新版本");
    }

    @Transactional
    public void delete(Long id) {
        BillingRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            return;
        }
        if (rule.getEnabled() == 1) {
            throw BizException.conflict("启用中的规则不能删除，请先切换其他规则");
        }
        ruleMapper.deleteById(id);
        logService.save("收费管理", "删除规则", "删除收费规则[" + rule.getName() + "]");
    }
}
