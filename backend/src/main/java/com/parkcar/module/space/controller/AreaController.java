package com.parkcar.module.space.controller;

import com.parkcar.common.Result;
import com.parkcar.module.space.entity.ParkingArea;
import com.parkcar.module.space.service.SpaceService;
import com.parkcar.security.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 区域管理接口
 */
@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
public class AreaController {

    private final SpaceService spaceService;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(spaceService.areaList());
    }

    @PostMapping
    @RequireRole("ADMIN")
    public Result<Void> create(@RequestBody ParkingArea area) {
        spaceService.areaCreate(area);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> update(@PathVariable Long id, @RequestBody ParkingArea area) {
        spaceService.areaUpdate(id, area);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> delete(@PathVariable Long id) {
        spaceService.areaDelete(id);
        return Result.ok();
    }
}
