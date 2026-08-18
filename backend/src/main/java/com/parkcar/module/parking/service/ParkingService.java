package com.parkcar.module.parking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkcar.common.BizException;
import com.parkcar.common.PageResult;
import com.parkcar.module.billing.entity.BillingOrder;
import com.parkcar.module.billing.entity.BillingRule;
import com.parkcar.module.billing.entity.PaymentRecord;
import com.parkcar.module.billing.mapper.BillingOrderMapper;
import com.parkcar.module.billing.mapper.BillingRuleMapper;
import com.parkcar.module.billing.mapper.PaymentRecordMapper;
import com.parkcar.module.billing.service.BillingService;
import com.parkcar.module.blacklist.entity.Blacklist;
import com.parkcar.module.blacklist.mapper.BlacklistMapper;
import com.parkcar.module.membership.entity.MembershipCard;
import com.parkcar.module.membership.mapper.MembershipCardMapper;
import com.parkcar.module.parking.entity.ParkingRecord;
import com.parkcar.module.parking.mapper.ParkingRecordMapper;
import com.parkcar.module.space.entity.ParkingArea;
import com.parkcar.module.space.entity.ParkingSpace;
import com.parkcar.module.space.mapper.ParkingAreaMapper;
import com.parkcar.module.space.mapper.ParkingSpaceMapper;
import com.parkcar.module.user.service.OperationLogService;
import com.parkcar.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 车辆出入场服务（核心）
 */
@Service
@RequiredArgsConstructor
public class ParkingService {

    private final ParkingRecordMapper recordMapper;
    private final ParkingSpaceMapper spaceMapper;
    private final ParkingAreaMapper areaMapper;
    private final BillingRuleMapper billingRuleMapper;
    private final BillingOrderMapper orderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final MembershipCardMapper cardMapper;
    private final BlacklistMapper blacklistMapper;
    private final BillingService billingService;
    private final OperationLogService logService;

    // ==================== 入场 ====================

    @Transactional
    public Map<String, Object> vehicleIn(String plateNo, Long spaceId, Long areaId, String remark) {
        plateNo = normalizePlate(plateNo);

        // 1. 校验重复入场
        Long exist = recordMapper.selectCount(new LambdaQueryWrapper<ParkingRecord>()
                .eq(ParkingRecord::getPlateNo, plateNo)
                .eq(ParkingRecord::getStatus, 0));
        if (exist != null && exist > 0) {
            throw BizException.conflict("车牌[" + plateNo + "]已在场内");
        }

        // 2. 黑名单校验（不拦截，仅预警）
        Blacklist black = blacklistMapper.selectOne(new LambdaQueryWrapper<Blacklist>()
                .eq(Blacklist::getPlateNo, plateNo)
                .eq(Blacklist::getStatus, 1));
        boolean inBlacklist = black != null;

        // 3. 月卡判断
        MembershipCard card = validCard(plateNo);

        // 4. 分配车位
        Long allocatedSpaceId = spaceId;
        boolean autoSpace = false;
        if (allocatedSpaceId == null) {
            ParkingSpace space = findFreeSpace(areaId);
            if (space == null) {
                throw BizException.conflict("无可用车位");
            }
            allocatedSpaceId = space.getId();
            autoSpace = true;
        } else {
            occupySpace(allocatedSpaceId);
        }

        // 5. 创建在场记录
        ParkingSpace space = spaceMapper.selectById(allocatedSpaceId);
        ParkingRecord record = new ParkingRecord();
        record.setPlateNo(plateNo);
        record.setSpaceId(allocatedSpaceId);
        record.setAreaId(space.getAreaId());
        record.setInTime(LocalDateTime.now());
        record.setStatus(0);
        record.setIsMember(card == null ? 0 : 1);
        record.setCardId(card == null ? null : card.getId());
        record.setChargeAmount(BigDecimal.ZERO);
        record.setPaidAmount(BigDecimal.ZERO);
        record.setDiscountAmount(BigDecimal.ZERO);
        record.setOperatorIn(UserContext.userId());
        record.setRemark(remark);
        recordMapper.insert(record);

        logService.save("出入场", "车辆入场", "车牌[" + plateNo + "]入场，车位[" + space.getSpaceNo() + "]"
                + (inBlacklist ? "，黑名单预警" : ""));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recordId", record.getId());
        data.put("plateNo", plateNo);
        data.put("spaceId", allocatedSpaceId);
        data.put("spaceNo", space.getSpaceNo());
        data.put("inTime", record.getInTime());
        data.put("isMember", card != null);
        data.put("inBlacklist", inBlacklist);
        data.put("autoSpace", autoSpace);
        return data;
    }

    // ==================== 查询 ====================

    public PageResult<Map<String, Object>> currentPage(long page, long size, String plateNo) {
        LambdaQueryWrapper<ParkingRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(ParkingRecord::getStatus, 0);
        qw.like(StringUtils.hasText(plateNo), ParkingRecord::getPlateNo, plateNo);
        qw.orderByDesc(ParkingRecord::getId);
        Page<ParkingRecord> p = recordMapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> records = p.getRecords().stream().map(this::toCurrentView).collect(Collectors.toList());
        return PageResult.of(p.getTotal(), page, size, records);
    }

    public PageResult<Map<String, Object>> historyPage(long page, long size, String plateNo,
                                                       LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<ParkingRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(ParkingRecord::getStatus, 1);
        qw.like(StringUtils.hasText(plateNo), ParkingRecord::getPlateNo, plateNo);
        qw.ge(startTime != null, ParkingRecord::getInTime, startTime);
        qw.le(endTime != null, ParkingRecord::getInTime, endTime);
        qw.orderByDesc(ParkingRecord::getId);
        Page<ParkingRecord> p = recordMapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> records = p.getRecords().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("plateNo", r.getPlateNo());
            m.put("inTime", r.getInTime());
            m.put("outTime", r.getOutTime());
            m.put("durationMinutes", durationMinutes(r));
            m.put("isMember", r.getIsMember());
            m.put("chargeAmount", r.getChargeAmount());
            m.put("paidAmount", r.getPaidAmount());
            m.put("discountAmount", r.getDiscountAmount());
            return m;
        }).collect(Collectors.toList());
        return PageResult.of(p.getTotal(), page, size, records);
    }

    // ==================== 出场 ====================

    public Map<String, Object> outPreview(String plateNo) {
        plateNo = normalizePlate(plateNo);
        ParkingRecord record = currentInRecord(plateNo);
        BillingRule rule = billingService.activeRule();
        boolean memberFree = isMemberFree(record);
        BigDecimal amount = billingService.calculate(record, rule);
        Map<String, Object> data = toPreview(record, rule, memberFree, amount);
        logService.save("出入场", "出场试算", "车牌[" + plateNo + "]试算金额" + amount);
        return data;
    }

    @Transactional
    public Map<String, Object> outSettle(Long recordId, Integer payType, BigDecimal discount, String remark) {
        // 行锁防止并发出场
        ParkingRecord record = recordMapper.selectInRecordForUpdate(recordId);
        if (record == null) {
            throw BizException.conflict("未找到在场记录或该车辆正在结算中");
        }
        BillingRule rule = billingService.activeRule();
        boolean memberFree = isMemberFree(record);
        BigDecimal amount = billingService.calculate(record, rule);
        discount = discount == null ? BigDecimal.ZERO : discount;
        if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(amount) > 0) {
            throw BizException.badRequest("减免金额不合法");
        }
        BigDecimal payable = amount.subtract(discount);
        Integer finalPayType = payType;
        if (memberFree) {
            payable = BigDecimal.ZERO;
            discount = BigDecimal.ZERO;
            finalPayType = 4; // 月卡抵扣
        }

        // 生成订单
        BillingOrder order = new BillingOrder();
        order.setOrderNo(genOrderNo());
        order.setRecordId(record.getId());
        order.setPlateNo(record.getPlateNo());
        order.setAmount(amount);
        order.setDiscount(discount);
        order.setPaidAmount(payable);
        order.setPayType(finalPayType);
        order.setStatus(1);
        order.setOperatorId(UserContext.userId());
        order.setPayTime(LocalDateTime.now());
        order.setRemark(remark);
        orderMapper.insert(order);

        // 支付流水
        if (finalPayType != null && finalPayType != 4) {
            PaymentRecord payment = new PaymentRecord();
            payment.setOrderId(order.getId());
            payment.setPayType(finalPayType);
            payment.setAmount(payable);
            payment.setStatus(1);
            payment.setOperatorId(UserContext.userId());
            payment.setPayTime(LocalDateTime.now());
            paymentRecordMapper.insert(payment);
        }

        // 更新停车记录
        ParkingRecord update = new ParkingRecord();
        update.setId(record.getId());
        update.setOutTime(LocalDateTime.now());
        update.setStatus(1);
        update.setBillingRuleId(rule.getId());
        update.setChargeAmount(amount);
        update.setPaidAmount(payable);
        update.setDiscountAmount(discount);
        update.setOperatorOut(UserContext.userId());
        recordMapper.updateById(update);

        // 释放车位
        if (record.getSpaceId() != null) {
            releaseSpace(record.getSpaceId());
        }

        String spaceNo = record.getSpaceId() == null ? "" : spaceNo(record.getSpaceId());
        logService.save("出入场", "车辆出场", "车牌[" + record.getPlateNo() + "]出场，实收" + payable
                + (finalPayType == 4 ? "(月卡)" : ""));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("recordId", record.getId());
        data.put("plateNo", record.getPlateNo());
        data.put("amount", amount);
        data.put("discount", discount);
        data.put("paidAmount", payable);
        data.put("payType", finalPayType);
        data.put("outTime", update.getOutTime());
        data.put("spaceNo", spaceNo);
        return data;
    }

    @Transactional
    public Map<String, Object> manualOut(Long recordId, String remark) {
        ParkingRecord record = recordMapper.selectById(recordId);
        if (record == null || record.getStatus() != 0) {
            throw BizException.conflict("未找到在场记录");
        }
        ParkingRecord update = new ParkingRecord();
        update.setId(record.getId());
        update.setOutTime(LocalDateTime.now());
        update.setStatus(2); // 异常完结
        update.setRemark(remark);
        recordMapper.updateById(update);
        if (record.getSpaceId() != null) {
            releaseSpace(record.getSpaceId());
        }
        logService.save("出入场", "手工出场", "车牌[" + record.getPlateNo() + "]手工完结");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recordId", record.getId());
        data.put("plateNo", record.getPlateNo());
        data.put("outTime", update.getOutTime());
        return data;
    }

    // ==================== 私有方法 ====================

    private String normalizePlate(String plateNo) {
        if (!StringUtils.hasText(plateNo)) {
            throw BizException.badRequest("车牌号不能为空");
        }
        return plateNo.trim().toUpperCase();
    }

    private ParkingRecord currentInRecord(String plateNo) {
        ParkingRecord record = recordMapper.selectOne(new LambdaQueryWrapper<ParkingRecord>()
                .eq(ParkingRecord::getPlateNo, plateNo)
                .eq(ParkingRecord::getStatus, 0));
        if (record == null) {
            throw BizException.notFound("未找到车牌[" + plateNo + "]的在场记录");
        }
        return record;
    }

    private MembershipCard validCard(String plateNo) {
        return cardMapper.selectOne(new LambdaQueryWrapper<MembershipCard>()
                .eq(MembershipCard::getPlateNo, plateNo)
                .eq(MembershipCard::getStatus, 1)
                .ge(MembershipCard::getEndTime, LocalDateTime.now())
                .last("LIMIT 1"));
    }

    private boolean isMemberFree(ParkingRecord record) {
        return record.getIsMember() != null && record.getIsMember() == 1;
    }

    private ParkingSpace findFreeSpace(Long areaId) {
        LambdaQueryWrapper<ParkingSpace> qw = new LambdaQueryWrapper<>();
        qw.eq(ParkingSpace::getStatus, 0);
        qw.eq(areaId != null, ParkingSpace::getAreaId, areaId);
        qw.orderByAsc(ParkingSpace::getSpaceNo);
        List<ParkingSpace> spaces = spaceMapper.selectList(qw);
        if (spaces.isEmpty()) {
            return null;
        }
        // 原子占位：状态=0 且 id=目标 时更新为占用，失败则换下一个
        for (ParkingSpace s : spaces) {
            int rows = spaceMapper.update(null, new LambdaUpdateWrapper<ParkingSpace>()
                    .eq(ParkingSpace::getId, s.getId())
                    .eq(ParkingSpace::getStatus, 0)
                    .set(ParkingSpace::getStatus, 1));
            if (rows > 0) {
                return s;
            }
        }
        return null;
    }

    private void occupySpace(Long spaceId) {
        int rows = spaceMapper.update(null, new LambdaUpdateWrapper<ParkingSpace>()
                .eq(ParkingSpace::getId, spaceId)
                .eq(ParkingSpace::getStatus, 0)
                .set(ParkingSpace::getStatus, 1));
        if (rows == 0) {
            throw BizException.conflict("车位不存在或已被占用");
        }
    }

    private void releaseSpace(Long spaceId) {
        spaceMapper.update(null, new LambdaUpdateWrapper<ParkingSpace>()
                .eq(ParkingSpace::getId, spaceId)
                .set(ParkingSpace::getStatus, 0));
    }

    private String spaceNo(Long spaceId) {
        ParkingSpace space = spaceMapper.selectById(spaceId);
        return space == null ? "" : space.getSpaceNo();
    }

    private String areaName(Long areaId) {
        ParkingArea area = areaMapper.selectById(areaId);
        return area == null ? "" : area.getName();
    }

    private long durationMinutes(ParkingRecord r) {
        LocalDateTime end = r.getOutTime() == null ? LocalDateTime.now() : r.getOutTime();
        return Math.max(0, Duration.between(r.getInTime(), end).toMinutes());
    }

    private Map<String, Object> toCurrentView(ParkingRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("plateNo", r.getPlateNo());
        m.put("spaceNo", r.getSpaceId() == null ? "" : spaceNo(r.getSpaceId()));
        m.put("areaName", r.getAreaId() == null ? "" : areaName(r.getAreaId()));
        m.put("inTime", r.getInTime());
        m.put("durationMinutes", durationMinutes(r));
        m.put("isMember", r.getIsMember());
        m.put("remark", r.getRemark());
        return m;
    }

    private Map<String, Object> toPreview(ParkingRecord r, BillingRule rule, boolean memberFree, BigDecimal amount) {
        Map<String, Object> ruleMap = new LinkedHashMap<>();
        ruleMap.put("ruleId", rule.getId());
        ruleMap.put("name", rule.getName());
        ruleMap.put("ruleType", rule.getRuleType());
        ruleMap.put("freeMinutes", rule.getFreeMinutes());
        ruleMap.put("firstHourFee", rule.getFirstHourFee());
        ruleMap.put("hourlyFee", rule.getHourlyFee());
        ruleMap.put("maxDailyFee", rule.getMaxDailyFee());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("recordId", r.getId());
        m.put("plateNo", r.getPlateNo());
        m.put("inTime", r.getInTime());
        m.put("outTime", LocalDateTime.now());
        m.put("durationMinutes", durationMinutes(r));
        m.put("isMember", r.getIsMember());
        m.put("memberFree", memberFree);
        m.put("spaceNo", r.getSpaceId() == null ? "" : spaceNo(r.getSpaceId()));
        m.put("rule", ruleMap);
        m.put("amount", amount);
        m.put("discount", BigDecimal.ZERO);
        m.put("payableAmount", amount);
        return m;
    }

    private String genOrderNo() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }
}
