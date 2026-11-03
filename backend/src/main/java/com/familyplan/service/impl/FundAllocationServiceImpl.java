package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.common.BusinessException;
import com.familyplan.dto.FundAllocationSummary;
import com.familyplan.entity.FundAllocation;
import com.familyplan.mapper.FundAllocationMapper;
import com.familyplan.security.SecurityUtil;
import com.familyplan.service.IFamilyMemberService;
import com.familyplan.service.IFundAllocationService;
import com.familyplan.support.FundAllocationRules;
import com.familyplan.support.FundAllocationRules.PeriodKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 资金分配：按成员和周期维护分类比例，合计不超过 100%。
 */
@Slf4j
@Service
public class FundAllocationServiceImpl extends ServiceImpl<FundAllocationMapper, FundAllocation>
        implements IFundAllocationService {

    @Autowired
    private IFamilyMemberService memberService;

    @Override
    public FundAllocationSummary summary(Long memberId, String period, Integer year, Integer month) {
        checkMember(memberId);
        PeriodKey key = FundAllocationRules.periodKey(period, year, month);
        List<FundAllocation> items = listBucket(memberId, key);
        items.sort(Comparator
                .comparing(FundAllocation::getPercentage, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(FundAllocation::getId, Comparator.nullsLast(Long::compareTo)));
        BigDecimal total = sum(items);
        FundAllocationSummary vo = new FundAllocationSummary();
        vo.setItems(items);
        vo.setTotalPercentage(total);
        BigDecimal remaining = FundAllocationRules.HUNDRED.subtract(total);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        vo.setRemainingPercentage(remaining);
        vo.setComplete(total.compareTo(FundAllocationRules.HUNDRED) == 0);
        return vo;
    }

    @Override
    public void add(FundAllocation item) {
        prepare(item, true);
        baseMapper.insert(item);
        log.info("添加资金分配成功: 成员={}, 分类={}, 比例={}", item.getMemberId(), item.getCategory(), item.getPercentage());
    }

    @Override
    public void update(FundAllocation item) {
        prepare(item, false);
        baseMapper.updateById(item);
        log.info("更新资金分配成功: ID={}", item.getId());
    }

    @Override
    public void delete(Long id) {
        FundAllocation existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("分配记录不存在");
        }
        checkMember(existing.getMemberId());
        baseMapper.deleteById(id);
        log.info("删除资金分配成功: ID={}", id);
    }

    private void prepare(FundAllocation item, boolean creating) {
        if (item == null) {
            throw new BusinessException("请填写分配内容");
        }
        if (item.getMemberId() == null) {
            throw new BusinessException("请选择家庭成员");
        }
        checkMember(item.getMemberId());
        if (!creating) {
            if (item.getId() == null) {
                throw new BusinessException("缺少分配记录");
            }
            FundAllocation existing = baseMapper.selectById(item.getId());
            if (existing == null) {
                throw new BusinessException("分配记录不存在");
            }
            checkMember(existing.getMemberId());
        }
        FundAllocationRules.normalize(item);
        assertUniqueAndSum(item);
    }

    private void assertUniqueAndSum(FundAllocation item) {
        PeriodKey key = new PeriodKey(item.getPeriod(), item.getYear(), item.getMonth());
        List<FundAllocation> siblings = listBucket(item.getMemberId(), key);
        BigDecimal others = BigDecimal.ZERO;
        for (FundAllocation sibling : siblings) {
            if (item.getId() != null && item.getId().equals(sibling.getId())) {
                continue;
            }
            if (item.getCategory().equals(sibling.getCategory())) {
                throw new BusinessException("该周期已有相同分类的分配，请直接编辑");
            }
            if (sibling.getPercentage() != null) {
                others = others.add(sibling.getPercentage());
            }
        }
        FundAllocationRules.assertWithinHundred(others, item.getPercentage());
    }

    private List<FundAllocation> listBucket(Long memberId, PeriodKey key) {
        LambdaQueryWrapper<FundAllocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundAllocation::getMemberId, memberId)
                .eq(FundAllocation::getPeriod, key.period)
                .eq(FundAllocation::getYear, key.year);
        if ("MONTH".equals(key.period)) {
            wrapper.eq(FundAllocation::getMonth, key.month);
        }
        wrapper.orderByDesc(FundAllocation::getPercentage).orderByAsc(FundAllocation::getId);
        return baseMapper.selectList(wrapper);
    }

    private void checkMember(Long memberId) {
        memberService.checkMemberBelongsToFamily(SecurityUtil.getRequiredFamilyId(), memberId);
    }

    private BigDecimal sum(List<FundAllocation> items) {
        return items.stream()
                .map(FundAllocation::getPercentage)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
