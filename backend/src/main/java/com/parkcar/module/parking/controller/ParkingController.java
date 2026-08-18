package com.parkcar.module.parking.controller;

import com.parkcar.common.PageResult;
import com.parkcar.common.Result;
import com.parkcar.module.parking.service.ParkingService;
import com.parkcar.security.RequireRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 车辆出入场接口
 */
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService parkingService;

    @Data
    public static class InRequest {
        @NotBlank(message = "车牌号不能为空")
        private String plateNo;
        private Long spaceId;
        private Long areaId;
        private String remark;
    }

    @Data
    public static class PreviewRequest {
        @NotBlank(message = "车牌号不能为空")
        private String plateNo;
    }

    @Data
    public static class SettleRequest {
        @NotNull(message = "停车记录ID不能为空")
        private Long recordId;
        private Integer payType;
        private BigDecimal discount;
        private String remark;
    }

    @Data
    public static class ManualOutRequest {
        private String remark;
    }

    /** 车辆入场 */
    @PostMapping("/in")
    @RequireRole({"ADMIN", "OPERATOR"})
    public Result<Map<String, Object>> in(@RequestBody InRequest req) {
        return Result.ok(parkingService.vehicleIn(req.getPlateNo(), req.getSpaceId(), req.getAreaId(), req.getRemark()));
    }

    /** 在场车辆 */
    @GetMapping("/current")
    public Result<PageResult<Map<String, Object>>> current(@RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "10") long size,
                                                           @RequestParam(required = false) String plateNo) {
        return Result.ok(parkingService.currentPage(page, size, plateNo));
    }

    /** 历史记录 */
    @GetMapping
    public Result<PageResult<Map<String, Object>>> history(@RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "10") long size,
                                                           @RequestParam(required = false) String plateNo,
                                                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(parkingService.historyPage(page, size, plateNo, startTime, endTime));
    }

    /** 出场试算 */
    @PostMapping("/out/preview")
    @RequireRole({"ADMIN", "OPERATOR"})
    public Result<Map<String, Object>> outPreview(@RequestBody PreviewRequest req) {
        return Result.ok(parkingService.outPreview(req.getPlateNo()));
    }

    /** 出场结算 */
    @PostMapping("/out/settle")
    @RequireRole({"ADMIN", "OPERATOR"})
    public Result<Map<String, Object>> outSettle(@RequestBody SettleRequest req) {
        return Result.ok(parkingService.outSettle(req.getRecordId(), req.getPayType(), req.getDiscount(), req.getRemark()));
    }

    /** 手工出场（异常完结） */
    @PostMapping("/{id}/manual-out")
    @RequireRole({"ADMIN", "OPERATOR"})
    public Result<Map<String, Object>> manualOut(@PathVariable Long id, @RequestBody(required = false) ManualOutRequest req) {
        String remark = req == null ? null : req.getRemark();
        return Result.ok(parkingService.manualOut(id, remark));
    }
}
