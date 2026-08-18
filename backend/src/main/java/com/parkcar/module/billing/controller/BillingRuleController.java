package com.parkcar.module.billing.controller;

import com.parkcar.common.Result;
import com.parkcar.module.billing.entity.BillingRule;
import com.parkcar.module.billing.service.BillingRuleService;
import com.parkcar.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收费规则接口
 */
@RestController
@RequestMapping("/api/billing-rules")
@RequiredArgsConstructor
public class BillingRuleController {

    private final BillingRuleService ruleService;

    @GetMapping
    public Result<List<BillingRule>> list() {
        return Result.ok(ruleService.list());
    }

    @GetMapping("/active")
    public Result<BillingRule> active() {
        return Result.ok(ruleService.active());
    }

    @PostMapping
    @RequireRole("ADMIN")
    public Result<Void> create(@RequestBody BillingRule rule) {
        ruleService.create(rule);
        return Result.ok();
    }

    @PostMapping("/{id}/enable")
    @RequireRole("ADMIN")
    public Result<Void> enable(@PathVariable Long id) {
        ruleService.enable(id);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> update(@PathVariable Long id, @RequestBody BillingRule rule) {
        ruleService.update(id, rule);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> delete(@PathVariable Long id) {
        ruleService.delete(id);
        return Result.ok();
    }
}
