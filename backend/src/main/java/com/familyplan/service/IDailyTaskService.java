package com.familyplan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.familyplan.entity.DailyTask;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日任务服务接口
 */
public interface IDailyTaskService extends IService<DailyTask> {

    /**
     * 按日期查询任务列表(家庭/工作)
     */
    List<DailyTask> listByDate(Long memberId, LocalDate date);

    /**
     * 分页查询任务记录
     */
    IPage<DailyTask> pageQuery(Long memberId, Integer page, Integer size,
                               String taskType, Integer completed,
                               LocalDate startDate, LocalDate endDate);

    /**
     * 添加任务
     */
    void add(DailyTask task);

    /**
     * 更新任务
     */
    void update(DailyTask task);

    /**
     * 打勾/取消完成
     */
    void complete(Long id, Integer completed);

    /**
     * 删除任务
     */
    void delete(Long id);
}
