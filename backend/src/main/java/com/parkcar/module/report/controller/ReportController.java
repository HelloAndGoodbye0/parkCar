package com.parkcar.module.report.controller;

import com.parkcar.common.Result;
import com.parkcar.module.report.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 统计报表接口
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/revenue")
    public Result<Map<String, Object>> revenue(@RequestParam(required = false) String startDate,
                                               @RequestParam(required = false) String endDate,
                                               @RequestParam(required = false) String granularity) {
        return Result.ok(reportService.revenue(startDate, endDate, granularity));
    }

    @GetMapping("/traffic")
    public Result<Map<String, Object>> traffic(@RequestParam(required = false) String startDate,
                                               @RequestParam(required = false) String endDate) {
        return Result.ok(reportService.traffic(startDate, endDate));
    }

    @GetMapping("/occupancy")
    public Result<Map<String, Object>> occupancy() {
        return Result.ok(reportService.occupancy());
    }

    @GetMapping("/revenue/export")
    public void export(@RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       @RequestParam(required = false) String granularity,
                       HttpServletResponse response) throws IOException {
        Map<String, Object> data = reportService.revenue(startDate, endDate, granularity);
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=revenue.csv");
        PrintWriter writer = response.getWriter();

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // BOM，Excel 打开不乱码
        sb.append("日期,订单数,营收(元)\r\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
        if (items != null) {
            for (Map<String, Object> item : items) {
                sb.append(item.get("date")).append(',').append(item.get("orderCount"))
                        .append(',').append(item.get("amount")).append("\r\n");
            }
        }
        writer.write(sb.toString());
        writer.flush();
    }
}
