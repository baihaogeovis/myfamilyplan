package com.familyplan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.familyplan.common.Result;
import com.familyplan.entity.ApprovalRecord;
import com.familyplan.service.IApprovalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 资金审批控制器
 */
@Slf4j
@RestController
@RequestMapping("/approval")
public class ApprovalController {

    @Autowired
    private IApprovalService approvalService;

    /**
     * 分页查询审批记录
     */
    @GetMapping("/page")
    public Result<IPage<ApprovalRecord>> page(@RequestParam(required = false) Long applicantId,
                                              @RequestParam(required = false) Long approverId,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(approvalService.pageQuery(applicantId, approverId, status, page, size));
    }

    /**
     * 提交审批申请
     */
    @PostMapping
    public Result<Void> apply(@RequestBody ApprovalRecord record) {
        approvalService.apply(record);
        return Result.success();
    }

    /**
     * 审批通过
     */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam Long approverId,
                                @RequestParam(required = false) String remark) {
        approvalService.approve(id, approverId, remark);
        return Result.success();
    }

    /**
     * 审批驳回
     */
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestParam Long approverId,
                               @RequestParam(required = false) String remark) {
        approvalService.reject(id, approverId, remark);
        return Result.success();
    }

    /**
     * 待审数量
     */
    @GetMapping("/pending/count")
    public Result<Map<String, Object>> pendingCount(@RequestParam(required = false) Long approverId) {
        long count = approvalService.pendingCount(approverId);
        return Result.success(Map.of("count", count));
    }

    /**
     * 删除审批记录
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        approvalService.delete(id);
        return Result.success();
    }
}
