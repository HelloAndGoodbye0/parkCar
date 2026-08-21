package com.parkcar.module.billing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkcar.common.BizException;
import com.parkcar.module.billing.dto.BillingDetail;
import com.parkcar.module.billing.entity.BillingRule;
import com.parkcar.module.billing.mapper.BillingRuleMapper;
import com.parkcar.module.parking.entity.ParkingRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 计费引擎
 */
@Service
@RequiredArgsConstructor
public class BillingService {

    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

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
     * 计算停车费用（按当前时间结算）
     */
    public BigDecimal calculate(LocalDateTime inTime, BillingRule rule, boolean freeFlag) {
        return calculate(inTime, LocalDateTime.now(), rule, freeFlag);
    }

    /**
     * 计算停车费用
     */
    public BigDecimal calculate(LocalDateTime inTime, LocalDateTime outTime, BillingRule rule, boolean freeFlag) {
        return calculateDetail(inTime, outTime, rule, freeFlag).getTotal();
    }

    /**
     * 计算停车费用并生成收费明细（按自然日切分，支持每日封顶、夜间计费）
     */
    public BillingDetail calculateDetail(LocalDateTime inTime, LocalDateTime outTime, BillingRule rule, boolean freeFlag) {
        List<BillingDetail.Item> items = new ArrayList<>();
        long totalMinutes = Math.max(0, Duration.between(inTime, outTime).toMinutes());
        int freeMinutes = rule.getFreeMinutes() == null ? 0 : rule.getFreeMinutes();

        if (freeFlag) {
            // 月卡有效期内免费
            addItem(items, "FREE", null, "月卡有效期内免费", BigDecimal.ZERO);
        } else if (totalMinutes <= freeMinutes) {
            // 免费时长内
            addItem(items, "FREE", null, "免费时长内停车，不收费", BigDecimal.ZERO);
        } else if (rule.getRuleType() != null && rule.getRuleType() == 1) {
            // 按次
            addItem(items, "ONCE", formatPeriod(inTime, outTime), "按次收费", nvl(rule.getFirstHourFee()));
        } else if (isNightEnabled(rule)) {
            // 按时 + 夜间计费
            calcByTimeWithNight(inTime, outTime, rule, freeMinutes, items);
        } else {
            // 按时（每日封顶）
            calcByTimeWithDailyCap(inTime, outTime, rule, freeMinutes, items);
        }

        BigDecimal total = items.stream()
                .map(BillingDetail.Item::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BillingDetail detail = new BillingDetail();
        detail.setTotal(total);
        detail.setItems(items);
        return detail;
    }

    /** 夜间计费是否启用（开始、结束、费用三字段齐全） */
    private boolean isNightEnabled(BillingRule rule) {
        return rule.getNightStart() != null && rule.getNightEnd() != null && rule.getNightFee() != null;
    }

    /**
     * 按时计费（按自然日切分，每日封顶）：
     * 将停车时长按自然日（00:00 为界）切分，每天独立按“首小时+每小时”计费并各自套用每日封顶；
     * 免费时长优先从入场起扣除。
     */
    private void calcByTimeWithDailyCap(LocalDateTime inTime, LocalDateTime outTime, BillingRule rule,
                                        int freeMinutes, List<BillingDetail.Item> items) {
        int remainingFree = freeMinutes;
        LocalDateTime cursor = inTime;
        while (cursor.isBefore(outTime)) {
            LocalDateTime dayEnd = cursor.toLocalDate().plusDays(1).atStartOfDay();
            if (dayEnd.isAfter(outTime)) {
                dayEnd = outTime;
            }
            long segMinutes = Duration.between(cursor, dayEnd).toMinutes();
            if (remainingFree > 0) {
                long take = Math.min(remainingFree, segMinutes);
                segMinutes -= take;
                remainingFree -= (int) take;
            }
            if (segMinutes > 0) {
                addDayItem(rule, items, cursor, dayEnd, segMinutes);
            }
            cursor = dayEnd;
        }
    }

    /** 单日按时费用：首小时 + 之后每小时向上取整，套用每日封顶，并生成一条明细 */
    private void addDayItem(BillingRule rule, List<BillingDetail.Item> items,
                            LocalDateTime start, LocalDateTime end, long minutes) {
        long hours = (minutes + 59) / 60; // 向上取整
        BigDecimal fee;
        if (hours <= 1) {
            fee = nvl(rule.getFirstHourFee());
        } else {
            fee = nvl(rule.getFirstHourFee())
                    .add(nvl(rule.getHourlyFee()).multiply(BigDecimal.valueOf(hours - 1)));
        }
        BigDecimal maxDaily = rule.getMaxDailyFee();
        boolean capped = maxDaily != null && fee.compareTo(maxDaily) > 0;
        if (capped) {
            fee = maxDaily;
        }
        String desc = formatMinutes(minutes);
        if (capped) {
            desc += "（已达每日封顶 ¥" + maxDaily.stripTrailingZeros().toPlainString() + "）";
        }
        addItem(items, "DAY", formatPeriod(start, end), desc, fee);
    }

    /**
     * 按时 + 夜间计费：
     * 1) 停车时段每覆盖一个夜间区间（每晚）计一笔夜间费用，支持跨午夜（如 22:00-次日06:00），
     *    跨午夜区间按“开始日”归属，只停后夜（如 05:00-05:30）也能命中；
     * 2) 白天部分按自然日切分，每天独立按按时规则计费并各自套用每日封顶；
     * 3) 免费时长优先从入场起的白天时段扣除。
     */
    private void calcByTimeWithNight(LocalDateTime inTime, LocalDateTime outTime, BillingRule rule,
                                     int freeMinutes, List<BillingDetail.Item> items) {
        LocalTime nightStart = rule.getNightStart();
        LocalTime nightEnd = rule.getNightEnd();
        boolean crossMidnight = nightStart.isAfter(nightEnd);

        // 统计覆盖的夜间区间数（跨午夜区间如 22:00-06:00 按“开始日”归属）
        int nightCount = 0;
        LocalDate day = inTime.toLocalDate().minusDays(1);
        LocalDate endDay = outTime.toLocalDate();
        while (!day.isAfter(endDay)) {
            LocalDateTime begin = LocalDateTime.of(day, nightStart);
            LocalDateTime finish = crossMidnight
                    ? LocalDateTime.of(day.plusDays(1), nightEnd)
                    : LocalDateTime.of(day, nightEnd);
            if (overlapMinutes(inTime, outTime, begin, finish) > 0) {
                nightCount++;
                String period = TIME_FMT.format(nightStart) + "~" + TIME_FMT.format(nightEnd)
                        + "（" + day.format(DAY_FMT) + "）";
                addItem(items, "NIGHT", period, "夜间第 " + nightCount + " 晚", nvl(rule.getNightFee()));
            }
            day = day.plusDays(1);
        }

        // 按自然日切分：每天剔除夜间分钟后，白天部分独立计费并封顶
        int remainingFree = freeMinutes;
        LocalDateTime cursor = inTime;
        while (cursor.isBefore(outTime)) {
            LocalDate dayStart = cursor.toLocalDate();
            LocalDateTime dayEnd = dayStart.plusDays(1).atStartOfDay();
            if (dayEnd.isAfter(outTime)) {
                dayEnd = outTime;
            }
            long segTotal = Duration.between(cursor, dayEnd).toMinutes();
            long nightMin = 0;
            if (crossMidnight) {
                // 前夜片段 [nightStart, 次日00:00)
                nightMin += overlapMinutes(cursor, dayEnd,
                        LocalDateTime.of(dayStart, nightStart), dayStart.plusDays(1).atStartOfDay());
                // 后夜片段 [00:00, nightEnd)
                nightMin += overlapMinutes(cursor, dayEnd,
                        LocalDateTime.of(dayStart, LocalTime.MIDNIGHT), LocalDateTime.of(dayStart, nightEnd));
            } else {
                nightMin += overlapMinutes(cursor, dayEnd,
                        LocalDateTime.of(dayStart, nightStart), LocalDateTime.of(dayStart, nightEnd));
            }
            long dayMin = Math.max(0, segTotal - nightMin);
            if (remainingFree > 0) {
                long take = Math.min(remainingFree, dayMin);
                dayMin -= take;
                remainingFree -= (int) take;
            }
            if (dayMin > 0) {
                addDayItem(rule, items, cursor, dayEnd, dayMin);
            }
            cursor = dayEnd;
        }
    }

    /** 两区间重叠分钟数，无交集返回 0 */
    private long overlapMinutes(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        LocalDateTime os = aStart.isAfter(bStart) ? aStart : bStart;
        LocalDateTime oe = aEnd.isBefore(bEnd) ? aEnd : bEnd;
        return os.isBefore(oe) ? Duration.between(os, oe).toMinutes() : 0;
    }

    public BigDecimal calculate(ParkingRecord record, BillingRule rule) {
        return calculate(record.getInTime(), rule, record.getIsMember() != null && record.getIsMember() == 1);
    }

    private void addItem(List<BillingDetail.Item> items, String type, String period, String desc, BigDecimal amount) {
        BillingDetail.Item item = new BillingDetail.Item();
        item.setType(type);
        item.setPeriod(period);
        item.setDesc(desc);
        item.setAmount(amount);
        items.add(item);
    }

    private String formatPeriod(LocalDateTime start, LocalDateTime end) {
        return start.format(PERIOD_FMT) + " ~ " + end.format(PERIOD_FMT);
    }

    private String formatMinutes(long minutes) {
        long h = minutes / 60;
        long m = minutes % 60;
        if (h > 0) {
            return m > 0 ? h + "小时" + m + "分钟" : h + "小时";
        }
        return m + "分钟";
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
