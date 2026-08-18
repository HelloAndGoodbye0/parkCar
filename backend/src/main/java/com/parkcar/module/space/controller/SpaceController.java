package com.parkcar.module.space.controller;

import com.parkcar.common.PageResult;
import com.parkcar.common.Result;
import com.parkcar.module.space.entity.ParkingSpace;
import com.parkcar.module.space.service.SpaceService;
import com.parkcar.security.RequireRole;
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
 * 车位管理接口
 */
@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    @Data
    public static class BatchCreateRequest {
        @NotNull(message = "区域不能为空")
        private Long areaId;
        private List<String> spaceNos;
        private Integer type;
    }

    @Data
    public static class StatusRequest {
        @NotNull(message = "状态不能为空")
        private Integer status;
    }

    @GetMapping
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "20") long size,
                                                        @RequestParam(required = false) Long areaId,
                                                        @RequestParam(required = false) Integer type,
                                                        @RequestParam(required = false) Integer status) {
        return Result.ok(spaceService.spacePage(page, size, areaId, type, status));
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(spaceService.spaceOverview());
    }

    @PostMapping
    @RequireRole("ADMIN")
    public Result<Void> create(@RequestBody ParkingSpace space) {
        spaceService.spaceCreate(space);
        return Result.ok();
    }

    @PostMapping("/batch")
    @RequireRole("ADMIN")
    public Result<Void> batchCreate(@RequestBody BatchCreateRequest req) {
        spaceService.spaceBatchCreate(req.getAreaId(), req.getSpaceNos(), req.getType());
        return Result.ok();
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> update(@PathVariable Long id, @RequestBody ParkingSpace space) {
        spaceService.spaceUpdate(id, space);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> delete(@PathVariable Long id) {
        spaceService.spaceDelete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @RequireRole("ADMIN")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        spaceService.spaceChangeStatus(id, req.getStatus());
        return Result.ok();
    }
}
