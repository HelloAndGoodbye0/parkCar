package com.parkcar.module.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkcar.common.BizException;
import com.parkcar.common.PageResult;
import com.parkcar.module.space.entity.ParkingArea;
import com.parkcar.module.space.entity.ParkingSpace;
import com.parkcar.module.space.mapper.ParkingAreaMapper;
import com.parkcar.module.space.mapper.ParkingSpaceMapper;
import com.parkcar.module.user.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 区域与车位服务
 */
@Service
@RequiredArgsConstructor
public class SpaceService {

    private final ParkingAreaMapper areaMapper;
    private final ParkingSpaceMapper spaceMapper;
    private final OperationLogService logService;

    // ==================== 区域 ====================

    public List<Map<String, Object>> areaList() {
        List<ParkingArea> areas = areaMapper.selectList(new LambdaQueryWrapper<ParkingArea>()
                .orderByAsc(ParkingArea::getSort));
        return areas.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("name", a.getName());
            m.put("location", a.getLocation());
            m.put("spaceCount", a.getSpaceCount());
            m.put("sort", a.getSort());
            m.put("status", a.getStatus());
            m.put("createTime", a.getCreateTime());
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void areaCreate(ParkingArea area) {
        area.setId(null);
        if (area.getSpaceCount() == null) {
            area.setSpaceCount(0);
        }
        area.setStatus(area.getStatus() == null ? 1 : area.getStatus());
        areaMapper.insert(area);
        logService.save("车位管理", "新增区域", "新增区域[" + area.getName() + "]");
    }

    @Transactional
    public void areaUpdate(Long id, ParkingArea area) {
        ParkingArea exist = areaMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("区域不存在");
        }
        ParkingArea update = new ParkingArea();
        update.setId(id);
        update.setName(area.getName());
        update.setLocation(area.getLocation());
        update.setSpaceCount(area.getSpaceCount());
        update.setSort(area.getSort());
        update.setStatus(area.getStatus());
        areaMapper.updateById(update);
        logService.save("车位管理", "修改区域", "修改区域[" + exist.getName() + "]");
    }

    @Transactional
    public void areaDelete(Long id) {
        ParkingArea exist = areaMapper.selectById(id);
        if (exist == null) {
            return;
        }
        Long spaceCount = spaceMapper.selectCount(new LambdaQueryWrapper<ParkingSpace>()
                .eq(ParkingSpace::getAreaId, id));
        if (spaceCount != null && spaceCount > 0) {
            throw BizException.conflict("该区域下还有车位，无法删除");
        }
        areaMapper.deleteById(id);
        logService.save("车位管理", "删除区域", "删除区域[" + exist.getName() + "]");
    }

    // ==================== 车位 ====================

    public PageResult<Map<String, Object>> spacePage(long page, long size, Long areaId, Integer type, Integer status) {
        LambdaQueryWrapper<ParkingSpace> qw = new LambdaQueryWrapper<>();
        qw.eq(areaId != null, ParkingSpace::getAreaId, areaId);
        qw.eq(type != null, ParkingSpace::getType, type);
        qw.eq(status != null, ParkingSpace::getStatus, status);
        qw.orderByAsc(ParkingSpace::getSpaceNo);
        Page<ParkingSpace> p = spaceMapper.selectPage(new Page<>(page, size), qw);

        Map<Long, String> areaNames = areaMapper.selectList(null).stream()
                .collect(Collectors.toMap(ParkingArea::getId, ParkingArea::getName));

        List<Map<String, Object>> records = p.getRecords().stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("areaId", s.getAreaId());
            m.put("areaName", areaNames.getOrDefault(s.getAreaId(), ""));
            m.put("spaceNo", s.getSpaceNo());
            m.put("type", s.getType());
            m.put("status", s.getStatus());
            m.put("remark", s.getRemark());
            return m;
        }).collect(Collectors.toList());
        return PageResult.of(p.getTotal(), page, size, records);
    }

    public Map<String, Object> spaceOverview() {
        List<ParkingSpace> all = spaceMapper.selectList(null);
        Map<Long, String> areaNames = areaMapper.selectList(null).stream()
                .collect(Collectors.toMap(ParkingArea::getId, ParkingArea::getName));

        long total = all.size();
        long free = all.stream().filter(s -> s.getStatus() == 0).count();
        long occupied = all.stream().filter(s -> s.getStatus() == 1).count();
        long disabled = all.stream().filter(s -> s.getStatus() == 2).count();
        long maintaining = all.stream().filter(s -> s.getStatus() == 3).count();

        Map<Long, List<ParkingSpace>> byArea = all.stream().collect(Collectors.groupingBy(ParkingSpace::getAreaId));
        List<Map<String, Object>> byAreaList = new ArrayList<>();
        byArea.forEach((areaId, spaces) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("areaId", areaId);
            item.put("areaName", areaNames.getOrDefault(areaId, ""));
            item.put("total", spaces.size());
            item.put("free", spaces.stream().filter(s -> s.getStatus() == 0).count());
            item.put("occupied", spaces.stream().filter(s -> s.getStatus() == 1).count());
            byAreaList.add(item);
        });

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("free", free);
        data.put("occupied", occupied);
        data.put("disabled", disabled);
        data.put("maintaining", maintaining);
        data.put("byArea", byAreaList);
        return data;
    }

    @Transactional
    public void spaceCreate(ParkingSpace space) {
        space.setId(null);
        space.setStatus(0);
        if (!StringUtils.hasText(space.getSpaceNo())) {
            throw BizException.badRequest("车位编号不能为空");
        }
        Long exists = spaceMapper.selectCount(new LambdaQueryWrapper<ParkingSpace>()
                .eq(ParkingSpace::getSpaceNo, space.getSpaceNo()));
        if (exists != null && exists > 0) {
            throw BizException.conflict("车位编号[" + space.getSpaceNo() + "]已存在");
        }
        spaceMapper.insert(space);
        refreshAreaCount(space.getAreaId());
        logService.save("车位管理", "新增车位", "新增车位[" + space.getSpaceNo() + "]");
    }

    @Transactional
    public void spaceBatchCreate(Long areaId, List<String> spaceNos, Integer type) {
        if (spaceNos == null || spaceNos.isEmpty()) {
            throw BizException.badRequest("车位编号列表不能为空");
        }
        for (String no : spaceNos) {
            ParkingSpace s = new ParkingSpace();
            s.setAreaId(areaId);
            s.setSpaceNo(no.trim());
            s.setType(type == null ? 0 : type);
            s.setStatus(0);
            Long exists = spaceMapper.selectCount(new LambdaQueryWrapper<ParkingSpace>()
                    .eq(ParkingSpace::getSpaceNo, s.getSpaceNo()));
            if (exists != null && exists > 0) {
                throw BizException.conflict("车位编号[" + s.getSpaceNo() + "]已存在");
            }
            spaceMapper.insert(s);
        }
        refreshAreaCount(areaId);
        logService.save("车位管理", "批量新增车位", "区域[" + areaId + "]批量新增" + spaceNos.size() + "个车位");
    }

    @Transactional
    public void spaceUpdate(Long id, ParkingSpace space) {
        ParkingSpace exist = spaceMapper.selectById(id);
        if (exist == null) {
            throw BizException.notFound("车位不存在");
        }
        Long duplicate = spaceMapper.selectCount(new LambdaQueryWrapper<ParkingSpace>()
                .eq(ParkingSpace::getSpaceNo, space.getSpaceNo())
                .ne(ParkingSpace::getId, id));
        if (duplicate != null && duplicate > 0) {
            throw BizException.conflict("车位编号[" + space.getSpaceNo() + "]已存在");
        }
        ParkingSpace update = new ParkingSpace();
        update.setId(id);
        update.setAreaId(space.getAreaId());
        update.setSpaceNo(space.getSpaceNo());
        update.setType(space.getType());
        update.setRemark(space.getRemark());
        spaceMapper.updateById(update);
        refreshAreaCount(exist.getAreaId());
        refreshAreaCount(space.getAreaId());
        logService.save("车位管理", "修改车位", "修改车位[" + exist.getSpaceNo() + "]");
    }

    @Transactional
    public void spaceDelete(Long id) {
        ParkingSpace space = spaceMapper.selectById(id);
        if (space == null) {
            return;
        }
        if (space.getStatus() == 1) {
            throw BizException.conflict("车位被占用，无法删除");
        }
        spaceMapper.deleteById(id);
        refreshAreaCount(space.getAreaId());
        logService.save("车位管理", "删除车位", "删除车位[" + space.getSpaceNo() + "]");
    }

    @Transactional
    public void spaceChangeStatus(Long id, Integer status) {
        ParkingSpace space = spaceMapper.selectById(id);
        if (space == null) {
            throw BizException.notFound("车位不存在");
        }
        if (space.getStatus() == 1 && (status == 2 || status == 3)) {
            throw BizException.conflict("车位被占用，无法禁用或维护");
        }
        ParkingSpace update = new ParkingSpace();
        update.setId(id);
        update.setStatus(status);
        spaceMapper.updateById(update);
        logService.save("车位管理", "调整车位状态", "车位[" + space.getSpaceNo() + "]状态->" + status);
    }

    private void refreshAreaCount(Long areaId) {
        if (areaId == null) {
            return;
        }
        Long count = spaceMapper.selectCount(new LambdaQueryWrapper<ParkingSpace>()
                .eq(ParkingSpace::getAreaId, areaId));
        ParkingArea update = new ParkingArea();
        update.setId(areaId);
        update.setSpaceCount(count == null ? 0 : count.intValue());
        areaMapper.updateById(update);
    }
}
