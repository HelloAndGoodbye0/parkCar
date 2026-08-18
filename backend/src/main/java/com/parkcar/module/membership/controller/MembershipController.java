package com.parkcar.module.membership.controller;

import com.parkcar.common.PageResult;
import com.parkcar.common.Result;
import com.parkcar.module.membership.entity.MembershipPackage;
import com.parkcar.module.membership.service.MembershipService;
import com.parkcar.security.RequireRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 会员月卡接口
 */
@RestController
@RequestMapping("/api/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @Data
    public static class CardCreateRequest {
        @NotBlank(message = "车牌号不能为空")
        private String plateNo;
        @NotNull(message = "套餐不能为空")
        private Long packageId;
        private String ownerName;
        private String ownerPhone;
    }

    @Data
    public static class RenewRequest {
        @NotNull(message = "套餐不能为空")
        private Long packageId;
    }

    @GetMapping("/packages")
    public Result<List<MembershipPackage>> packages() {
        return Result.ok(membershipService.packages());
    }

    @PostMapping("/packages")
    @RequireRole("ADMIN")
    public Result<Void> packageCreate(@RequestBody MembershipPackage pkg) {
        membershipService.packageCreate(pkg);
        return Result.ok();
    }

    @PutMapping("/packages/{id}")
    @RequireRole("ADMIN")
    public Result<Void> packageUpdate(@PathVariable Long id, @RequestBody MembershipPackage pkg) {
        membershipService.packageUpdate(id, pkg);
        return Result.ok();
    }

    @GetMapping("/cards")
    public Result<PageResult<Map<String, Object>>> cards(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) String plateNo,
                                                         @RequestParam(required = false) Integer status) {
        return Result.ok(membershipService.cards(page, size, plateNo, status));
    }

    @GetMapping("/cards/plate/{plateNo}")
    public Result<Map<String, Object>> cardByPlate(@PathVariable String plateNo) {
        return Result.ok(membershipService.cardByPlate(plateNo));
    }

    @PostMapping("/cards")
    @RequireRole({"ADMIN", "OPERATOR"})
    public Result<Map<String, Object>> cardCreate(@RequestBody CardCreateRequest req) {
        return Result.ok(membershipService.cardCreate(
                req.getPlateNo(), req.getPackageId(), req.getOwnerName(), req.getOwnerPhone()));
    }

    @PostMapping("/cards/{id}/renew")
    @RequireRole({"ADMIN", "OPERATOR"})
    public Result<Map<String, Object>> cardRenew(@PathVariable Long id, @RequestBody RenewRequest req) {
        return Result.ok(membershipService.cardRenew(id, req.getPackageId()));
    }

    @DeleteMapping("/cards/{id}")
    @RequireRole("ADMIN")
    public Result<Void> cardCancel(@PathVariable Long id) {
        membershipService.cardCancel(id);
        return Result.ok();
    }
}
