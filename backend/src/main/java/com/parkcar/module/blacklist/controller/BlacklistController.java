package com.parkcar.module.blacklist.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkcar.common.BizException;
import com.parkcar.common.PageResult;
import com.parkcar.common.Result;
import com.parkcar.module.blacklist.entity.Blacklist;
import com.parkcar.module.blacklist.mapper.BlacklistMapper;
import com.parkcar.module.user.service.OperationLogService;
import com.parkcar.security.RequireRole;
import com.parkcar.security.UserContext;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 黑名单接口
 */
@RestController
@RequestMapping("/api/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistMapper blacklistMapper;
    private final OperationLogService logService;

    @Data
    public static class CreateRequest {
        @NotBlank(message = "车牌号不能为空")
        private String plateNo;
        private String reason;
    }

    @GetMapping
    public Result<PageResult<Blacklist>> page(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String plateNo) {
        LambdaQueryWrapper<Blacklist> qw = new LambdaQueryWrapper<>();
        qw.eq(Blacklist::getStatus, 1);
        qw.like(StringUtils.hasText(plateNo), Blacklist::getPlateNo, plateNo);
        qw.orderByDesc(Blacklist::getId);
        Page<Blacklist> p = blacklistMapper.selectPage(new Page<>(page, size), qw);
        return Result.ok(PageResult.of(p.getTotal(), page, size, p.getRecords()));
    }

    @PostMapping
    @RequireRole("ADMIN")
    public Result<Void> create(@RequestBody CreateRequest req) {
        String plateNo = req.getPlateNo().trim().toUpperCase();
        Long exists = blacklistMapper.selectCount(new LambdaQueryWrapper<Blacklist>()
                .eq(Blacklist::getPlateNo, plateNo)
                .eq(Blacklist::getStatus, 1));
        if (exists != null && exists > 0) {
            throw BizException.conflict("该车牌已在黑名单中");
        }
        Blacklist black = new Blacklist();
        black.setPlateNo(plateNo);
        black.setReason(req.getReason());
        black.setStatus(1);
        black.setCreateBy(UserContext.userId());
        blacklistMapper.insert(black);
        logService.save("黑名单", "加入黑名单", "车牌[" + plateNo + "]加入黑名单");
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> delete(@PathVariable Long id) {
        Blacklist black = blacklistMapper.selectById(id);
        if (black == null) {
            return Result.ok();
        }
        Blacklist update = new Blacklist();
        update.setId(id);
        update.setStatus(0);
        blacklistMapper.updateById(update);
        logService.save("黑名单", "解除黑名单", "车牌[" + black.getPlateNo() + "]解除黑名单");
        return Result.ok();
    }
}
