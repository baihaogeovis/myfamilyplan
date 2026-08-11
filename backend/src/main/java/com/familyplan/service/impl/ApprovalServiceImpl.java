package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.common.BusinessException;
import com.familyplan.entity.ApprovalRecord;
import com.familyplan.mapper.ApprovalMapper;
import com.familyplan.service.IApprovalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 资金审批服务实现
 */
@Slf4j
@Service
public class ApprovalServiceImpl extends ServiceImpl<ApprovalMapper, ApprovalRecord> implements IApprovalService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    @Override
    public IPage<ApprovalRecord> pageQuery(Long applicantId, Long approverId,
                                            String status, Integer page, Integer size) {
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        if (applicantId != null) {
            wrapper.eq(ApprovalRecord::getApplicantId, applicantId);
        }
        if (approverId != null) {
            wrapper.eq(ApprovalRecord::getApproverId, approverId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ApprovalRecord::getStatus, status);
        }
        wrapper.orderByDesc(ApprovalRecord::getCreateTime);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void apply(ApprovalRecord record) {
        if (record.getStatus() == null || record.getStatus().isEmpty()) {
            record.setStatus(PENDING);
        }
        baseMapper.insert(record);
        log.info("提交资金审批申请成功: 申请人={}, 金额={}", record.getApplicantId(), record.getAmount());
    }

    @Override
    public void approve(Long id, Long approverId, String remark) {
        ApprovalRecord record = baseMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("审批记录不存在");
        }
        if (!PENDING.equals(record.getStatus())) {
            throw new BusinessException("该申请已处理，无法重复审批");
        }
        record.setStatus(APPROVED);
        record.setApproverId(approverId);
        record.setRemark(remark);
        record.setApproveTime(LocalDateTime.now());
        baseMapper.updateById(record);
        log.info("审批通过: ID={}, 审批人={}", id, approverId);
    }

    @Override
    public void reject(Long id, Long approverId, String remark) {
        ApprovalRecord record = baseMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("审批记录不存在");
        }
        if (!PENDING.equals(record.getStatus())) {
            throw new BusinessException("该申请已处理，无法重复审批");
        }
        record.setStatus(REJECTED);
        record.setApproverId(approverId);
        record.setRemark(remark);
        record.setApproveTime(LocalDateTime.now());
        baseMapper.updateById(record);
        log.info("审批驳回: ID={}, 审批人={}", id, approverId);
    }

    @Override
    public long pendingCount(Long approverId) {
        LambdaQueryWrapper<ApprovalRecord> wrapper = new LambdaQueryWrapper<>();
        if (approverId != null) {
            wrapper.eq(ApprovalRecord::getApproverId, approverId);
        }
        wrapper.eq(ApprovalRecord::getStatus, PENDING);
        return baseMapper.selectCount(wrapper);
    }

    @Override
    public void delete(Long id) {
        baseMapper.deleteById(id);
        log.info("删除审批记录成功: ID={}", id);
    }
}
