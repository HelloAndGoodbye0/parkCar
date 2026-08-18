package com.parkcar.module.billing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkcar.common.PageResult;
import com.parkcar.common.Result;
import com.parkcar.module.billing.entity.BillingOrder;
import com.parkcar.module.billing.entity.PaymentRecord;
import com.parkcar.module.billing.mapper.BillingOrderMapper;
import com.parkcar.module.billing.mapper.PaymentRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单与流水接口
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final BillingOrderMapper orderMapper;
    private final PaymentRecordMapper paymentRecordMapper;

    @GetMapping
    public Result<PageResult<BillingOrder>> page(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String plateNo,
                                                 @RequestParam(required = false) Integer payType,
                                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        LambdaQueryWrapper<BillingOrder> qw = new LambdaQueryWrapper<>();
        qw.like(StringUtils.hasText(plateNo), BillingOrder::getPlateNo, plateNo);
        qw.eq(payType != null, BillingOrder::getPayType, payType);
        qw.ge(startTime != null, BillingOrder::getCreateTime, startTime);
        qw.le(endTime != null, BillingOrder::getCreateTime, endTime);
        qw.orderByDesc(BillingOrder::getId);
        Page<BillingOrder> p = orderMapper.selectPage(new Page<>(page, size), qw);
        return Result.ok(PageResult.of(p.getTotal(), page, size, p.getRecords()));
    }

    @GetMapping("/{orderNo}")
    public Result<Map<String, Object>> detail(@PathVariable String orderNo) {
        BillingOrder order = orderMapper.selectOne(new LambdaQueryWrapper<BillingOrder>()
                .eq(BillingOrder::getOrderNo, orderNo));
        if (order == null) {
            return Result.fail(40400, "订单不存在");
        }
        List<PaymentRecord> payments = paymentRecordMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, order.getId()));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("order", order);
        data.put("payments", payments);
        return Result.ok(data);
    }
}
