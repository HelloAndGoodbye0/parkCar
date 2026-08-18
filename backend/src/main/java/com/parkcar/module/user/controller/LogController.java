package com.parkcar.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkcar.common.PageResult;
import com.parkcar.common.Result;
import com.parkcar.module.user.entity.OperationLog;
import com.parkcar.module.user.mapper.OperationLogMapper;
import com.parkcar.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 操作日志接口
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogMapper operationLogMapper;

    @GetMapping
    @RequireRole("ADMIN")
    public Result<PageResult<OperationLog>> page(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String username,
                                                 @RequestParam(required = false) String module,
                                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLog> qw = new LambdaQueryWrapper<>();
        qw.eq(StringUtils.hasText(username), OperationLog::getUsername, username);
        qw.eq(StringUtils.hasText(module), OperationLog::getModule, module);
        qw.ge(startTime != null, OperationLog::getCreateTime, startTime);
        qw.le(endTime != null, OperationLog::getCreateTime, endTime);
        qw.orderByDesc(OperationLog::getId);
        Page<OperationLog> p = operationLogMapper.selectPage(new Page<>(page, size), qw);
        return Result.ok(PageResult.of(p.getTotal(), page, size, p.getRecords()));
    }
}
