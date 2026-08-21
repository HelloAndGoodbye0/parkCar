package com.parkcar.module.membership.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkcar.common.BizException;
import com.parkcar.common.PageResult;
import com.parkcar.module.billing.entity.BillingOrder;
import com.parkcar.module.billing.mapper.BillingOrderMapper;
import com.parkcar.module.membership.entity.MembershipCard;
import com.parkcar.module.membership.entity.MembershipPackage;
import com.parkcar.module.membership.mapper.MembershipCardMapper;
import com.parkcar.module.membership.mapper.MembershipPackageMapper;
import com.parkcar.module.user.service.OperationLogService;
import com.parkcar.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 会员月卡服务
 */
@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipPackageMapper packageMapper;
    private final MembershipCardMapper cardMapper;
    private final BillingOrderMapper orderMapper;
    private final OperationLogService logService;

    // ==================== 套餐 ====================

    public List<MembershipPackage> packages(Boolean all) {
        LambdaQueryWrapper<MembershipPackage> qw = new LambdaQueryWrapper<>();
        // all=true 返回全部（管理列表）；否则只返回上架且在活动时间内的套餐（办理/续费入口）
        if (all == null || !all) {
            LocalDateTime now = LocalDateTime.now();
            qw.eq(MembershipPackage::getStatus, 1)
                    .and(w -> w.isNull(MembershipPackage::getStartTime)
                            .or().le(MembershipPackage::getStartTime, now))
                    .and(w -> w.isNull(MembershipPackage::getEndTime)
                            .or().ge(MembershipPackage::getEndTime, now));
        }
        qw.orderByAsc(MembershipPackage::getPrice);
        return packageMapper.selectList(qw);
    }

    public void packageCreate(MembershipPackage pkg) {
        pkg.setId(null);
        pkg.setStatus(1);
        packageMapper.insert(pkg);
        logService.save("会员月卡", "新增套餐", "新增套餐[" + pkg.getName() + "]");
    }

    public void packageUpdate(Long id, MembershipPackage pkg) {
        // 用 UpdateWrapper 更新，startTime/endTime 传 null 时也能清空（恢复长期有效）
        LambdaUpdateWrapper<MembershipPackage> uw = new LambdaUpdateWrapper<>();
        uw.eq(MembershipPackage::getId, id)
                .set(MembershipPackage::getName, pkg.getName())
                .set(MembershipPackage::getDurationDays, pkg.getDurationDays())
                .set(MembershipPackage::getPrice, pkg.getPrice())
                .set(MembershipPackage::getStatus, pkg.getStatus())
                .set(MembershipPackage::getStartTime, pkg.getStartTime())
                .set(MembershipPackage::getEndTime, pkg.getEndTime())
                .set(MembershipPackage::getRemark, pkg.getRemark());
        packageMapper.update(null, uw);
        logService.save("会员月卡", "修改套餐", "修改套餐[" + pkg.getName() + "]");
    }

    public void packageDelete(Long id) {
        MembershipPackage pkg = packageMapper.selectById(id);
        if (pkg == null) {
            throw BizException.conflict("套餐不存在");
        }
        Long used = cardMapper.selectCount(new LambdaQueryWrapper<MembershipCard>()
                .eq(MembershipCard::getPackageId, id));
        if (used != null && used > 0) {
            throw BizException.conflict("套餐[" + pkg.getName() + "]已被" + used + "张月卡使用，无法删除，可改为下架");
        }
        packageMapper.deleteById(id);
        logService.save("会员月卡", "删除套餐", "删除套餐[" + pkg.getName() + "]");
    }

    // ==================== 月卡 ====================

    public PageResult<Map<String, Object>> cards(long page, long size, String plateNo, Integer status) {
        LambdaQueryWrapper<MembershipCard> qw = new LambdaQueryWrapper<>();
        qw.like(StringUtils.hasText(plateNo), MembershipCard::getPlateNo, plateNo);
        qw.eq(status != null, MembershipCard::getStatus, status);
        qw.orderByDesc(MembershipCard::getId);
        Page<MembershipCard> p = cardMapper.selectPage(new Page<>(page, size), qw);

        Map<Long, String> pkgNames = packageMapper.selectList(null).stream()
                .collect(Collectors.toMap(MembershipPackage::getId, MembershipPackage::getName));

        List<Map<String, Object>> records = p.getRecords().stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("plateNo", c.getPlateNo());
            m.put("packageId", c.getPackageId());
            m.put("packageName", c.getPackageId() == null ? "" : pkgNames.getOrDefault(c.getPackageId(), ""));
            m.put("ownerName", c.getOwnerName());
            m.put("ownerPhone", c.getOwnerPhone());
            m.put("startTime", c.getStartTime());
            m.put("endTime", c.getEndTime());
            m.put("status", c.getStatus());
            return m;
        }).collect(Collectors.toList());
        return PageResult.of(p.getTotal(), page, size, records);
    }

    public Map<String, Object> cardByPlate(String plateNo) {
        MembershipCard card = cardMapper.selectOne(new LambdaQueryWrapper<MembershipCard>()
                .eq(MembershipCard::getPlateNo, plateNo)
                .eq(MembershipCard::getStatus, 1)
                .orderByDesc(MembershipCard::getId)
                .last("LIMIT 1"));
        if (card == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", card.getId());
        m.put("plateNo", card.getPlateNo());
        m.put("ownerName", card.getOwnerName());
        m.put("startTime", card.getStartTime());
        m.put("endTime", card.getEndTime());
        m.put("status", card.getStatus());
        return m;
    }

    @Transactional
    public Map<String, Object> cardCreate(String plateNo, Long packageId, String ownerName, String ownerPhone) {
        plateNo = plateNo.trim().toUpperCase();
        MembershipPackage pkg = packageMapper.selectById(packageId);
        if (pkg == null) {
            throw BizException.notFound("套餐不存在");
        }
        checkPackageAvailable(pkg);
        Long active = cardMapper.selectCount(new LambdaQueryWrapper<MembershipCard>()
                .eq(MembershipCard::getPlateNo, plateNo)
                .eq(MembershipCard::getStatus, 1));
        if (active != null && active > 0) {
            throw BizException.conflict("车牌[" + plateNo + "]已有有效月卡，请续费");
        }
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(pkg.getDurationDays());

        MembershipCard card = new MembershipCard();
        card.setPlateNo(plateNo);
        card.setPackageId(packageId);
        card.setOwnerName(ownerName);
        card.setOwnerPhone(ownerPhone);
        card.setStartTime(start);
        card.setEndTime(end);
        card.setStatus(1);
        cardMapper.insert(card);

        // 生成月卡订单
        BillingOrder order = new BillingOrder();
        order.setOrderNo(genOrderNo());
        order.setPlateNo(plateNo);
        order.setAmount(pkg.getPrice());
        order.setDiscount(BigDecimal.ZERO);
        order.setPaidAmount(pkg.getPrice());
        order.setPayType(1); // 现金办理，简化
        order.setStatus(1);
        order.setOperatorId(UserContext.userId());
        order.setPayTime(LocalDateTime.now());
        order.setRemark("办理月卡[" + pkg.getName() + "]");
        orderMapper.insert(order);

        logService.save("会员月卡", "办理月卡", "车牌[" + plateNo + "]办理" + pkg.getName());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cardId", card.getId());
        data.put("plateNo", plateNo);
        data.put("startTime", start);
        data.put("endTime", end);
        data.put("amount", pkg.getPrice());
        data.put("orderNo", order.getOrderNo());
        return data;
    }

    @Transactional
    public Map<String, Object> cardRenew(Long cardId, Long packageId) {
        MembershipCard card = cardMapper.selectById(cardId);
        if (card == null) {
            throw BizException.notFound("月卡不存在");
        }
        MembershipPackage pkg = packageMapper.selectById(packageId);
        if (pkg == null) {
            throw BizException.notFound("套餐不存在");
        }
        checkPackageAvailable(pkg);
        // 从原到期时间或当前时间续期
        LocalDateTime base = card.getEndTime() != null && card.getEndTime().isAfter(LocalDateTime.now())
                ? card.getEndTime() : LocalDateTime.now();
        LocalDateTime newEnd = base.plusDays(pkg.getDurationDays());

        MembershipCard update = new MembershipCard();
        update.setId(cardId);
        update.setEndTime(newEnd);
        update.setStatus(1);
        cardMapper.updateById(update);

        BillingOrder order = new BillingOrder();
        order.setOrderNo(genOrderNo());
        order.setPlateNo(card.getPlateNo());
        order.setAmount(pkg.getPrice());
        order.setDiscount(BigDecimal.ZERO);
        order.setPaidAmount(pkg.getPrice());
        order.setPayType(1);
        order.setStatus(1);
        order.setOperatorId(UserContext.userId());
        order.setPayTime(LocalDateTime.now());
        order.setRemark("月卡续费[" + pkg.getName() + "]");
        orderMapper.insert(order);

        logService.save("会员月卡", "月卡续费", "车牌[" + card.getPlateNo() + "]续费" + pkg.getName());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cardId", cardId);
        data.put("plateNo", card.getPlateNo());
        data.put("endTime", newEnd);
        data.put("amount", pkg.getPrice());
        data.put("orderNo", order.getOrderNo());
        return data;
    }

    @Transactional
    public void cardCancel(Long cardId) {
        MembershipCard card = cardMapper.selectById(cardId);
        if (card == null) {
            return;
        }
        MembershipCard update = new MembershipCard();
        update.setId(cardId);
        update.setStatus(2);
        cardMapper.updateById(update);
        logService.save("会员月卡", "退订月卡", "退订车牌[" + card.getPlateNo() + "]月卡");
    }

    /** 校验套餐当前是否可办理/续费（上架且在活动时间内） */
    private void checkPackageAvailable(MembershipPackage pkg) {
        if (pkg.getStatus() == null || pkg.getStatus() != 1) {
            throw BizException.conflict("套餐[" + pkg.getName() + "]已下架，暂不可办理");
        }
        LocalDateTime now = LocalDateTime.now();
        if (pkg.getStartTime() != null && pkg.getStartTime().isAfter(now)) {
            throw BizException.conflict("套餐[" + pkg.getName() + "]活动尚未开始");
        }
        if (pkg.getEndTime() != null && pkg.getEndTime().isBefore(now)) {
            throw BizException.conflict("套餐[" + pkg.getName() + "]活动已结束");
        }
    }

    private String genOrderNo() {
        return "M" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }
}
