package com.parkcar.module.report.service;

import com.parkcar.module.report.mapper.ReportMapper;
import com.parkcar.module.space.entity.ParkingSpace;
import com.parkcar.module.space.mapper.ParkingSpaceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计报表服务
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final ParkingSpaceMapper spaceMapper;

    /**
     * 营收统计
     */
    public Map<String, Object> revenue(String startDate, String endDate, String granularity) {
        String start = normalize(startDate, "2026-01-01 00:00:00");
        String end = normalizeEnd(endDate);
        String fmt = switch (granularity == null ? "day" : granularity) {
            case "month" -> "%Y-%m";
            case "year" -> "%Y";
            default -> "%Y-%m-%d";
        };

        List<Map<String, Object>> items = reportMapper.revenueByDate(start, end, fmt);
        List<Map<String, Object>> payTypes = reportMapper.revenueByPayType(start, end);

        BigDecimal total = BigDecimal.ZERO;
        long orderCount = 0;
        for (Map<String, Object> item : items) {
            total = total.add(toDecimal(item.get("amount")));
            orderCount += ((Number) item.getOrDefault("orderCount", 0)).longValue();
        }

        Map<String, Object> byPayType = new LinkedHashMap<>();
        byPayType.put("cash", BigDecimal.ZERO);
        byPayType.put("wechat", BigDecimal.ZERO);
        byPayType.put("alipay", BigDecimal.ZERO);
        byPayType.put("card", BigDecimal.ZERO);
        for (Map<String, Object> pt : payTypes) {
            int type = ((Number) pt.get("pay_type")).intValue();
            BigDecimal amount = toDecimal(pt.get("amount"));
            switch (type) {
                case 1 -> byPayType.put("cash", amount);
                case 2 -> byPayType.put("wechat", amount);
                case 3 -> byPayType.put("alipay", amount);
                case 4 -> byPayType.put("card", amount);
                default -> {
                }
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalAmount", total);
        data.put("orderCount", orderCount);
        data.put("byPayType", byPayType);
        data.put("items", items);
        return data;
    }

    /**
     * 车流统计
     */
    public Map<String, Object> traffic(String startDate, String endDate) {
        String start = normalize(startDate, "2026-01-01 00:00:00");
        String end = normalizeEnd(endDate);
        List<Map<String, Object>> inList = reportMapper.inTrafficByDate(start, end);
        List<Map<String, Object>> outList = reportMapper.outTrafficByDate(start, end);

        Map<String, Object> inMap = new LinkedHashMap<>();
        Map<String, Object> outMap = new LinkedHashMap<>();
        for (Map<String, Object> m : inList) {
            inMap.put(String.valueOf(m.get("date")), m.get("count"));
        }
        for (Map<String, Object> m : outList) {
            outMap.put(String.valueOf(m.get("date")), m.get("count"));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("inMap", inMap);
        data.put("outMap", outMap);
        return data;
    }

    /**
     * 车位利用率
     */
    public Map<String, Object> occupancy() {
        List<ParkingSpace> all = spaceMapper.selectList(null);
        long total = all.size();
        long occupied = all.stream().filter(s -> s.getStatus() == 1).count();
        long free = all.stream().filter(s -> s.getStatus() == 0).count();
        double rate = total == 0 ? 0 : (double) occupied / total;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("occupied", occupied);
        data.put("free", free);
        data.put("occupancyRate", BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP));
        return data;
    }

    private String normalize(String date, String defaultValue) {
        if (date == null || date.isBlank()) {
            return defaultValue;
        }
        String d = date.trim();
        if (d.length() == 10) {
            return d + " 00:00:00";
        }
        return d;
    }

    private String normalizeEnd(String date) {
        if (date == null || date.isBlank()) {
            return "2099-12-31 23:59:59";
        }
        String d = date.trim();
        if (d.length() == 10) {
            return d + " 23:59:59";
        }
        return d;
    }

    private BigDecimal toDecimal(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
