package com.familyplan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.entity.ApprovalRecord;

/**
 * 资金审批服务接口
 */
public interface IApprovalService extends IService<ApprovalRecord> {

    /**
     * 分页查询审批记录
     */
    IPage<ApprovalRecord> pageQuery(Long applicantId, Long approverId,
                                     String status, Integer page, Integer size);

    /**
     * 提交审批申请
     */
    void apply(ApprovalRecord record);

    /**
     * 审批通过
     */
    void approve(Long id, Long approverId, String remark);

    /**
     * 审批驳回
     */
    void reject(Long id, Long approverId, String remark);

    /**
     * 待审数量
     */
    long pendingCount(Long approverId);

    /**
     * 删除审批记录
     */
    void delete(Long id);
}
