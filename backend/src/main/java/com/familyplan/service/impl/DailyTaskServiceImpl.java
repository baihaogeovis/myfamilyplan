package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.familyplan.entity.DailyTask;
import com.familyplan.mapper.DailyTaskMapper;
import com.familyplan.service.IDailyTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日任务服务实现
 */
@Slf4j
@Service
public class DailyTaskServiceImpl extends ServiceImpl<DailyTaskMapper, DailyTask> implements IDailyTaskService {

    @Override
    public List<DailyTask> listByDate(Long memberId, LocalDate date) {
        LambdaQueryWrapper<DailyTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyTask::getMemberId, memberId)
                .eq(DailyTask::getTaskDate, date)
                .orderByAsc(DailyTask::getCompleted)
                .orderByAsc(DailyTask::getId);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public IPage<DailyTask> pageQuery(Long memberId, Integer page, Integer size,
                                      String taskType, Integer completed,
                                      LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<DailyTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyTask::getMemberId, memberId);
        if (taskType != null && !taskType.isEmpty()) {
            wrapper.eq(DailyTask::getTaskType, taskType);
        }
        if (completed != null) {
            wrapper.eq(DailyTask::getCompleted, completed);
        }
        if (startDate != null) {
            wrapper.ge(DailyTask::getTaskDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(DailyTask::getTaskDate, endDate);
        }
        wrapper.orderByDesc(DailyTask::getTaskDate).orderByAsc(DailyTask::getCompleted);
        return baseMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void add(DailyTask task) {
        if (task.getTaskDate() == null) {
            task.setTaskDate(LocalDate.now());
        }
        if (task.getCompleted() == null) {
            task.setCompleted(0);
        }
        if (task.getTaskType() == null || task.getTaskType().isEmpty()) {
            task.setTaskType("FAMILY");
        }
        if (task.getPriority() == null || task.getPriority().isEmpty()) {
            task.setPriority("MEDIUM");
        }
        baseMapper.insert(task);
        log.info("添加每日任务成功: {}", task.getTaskName());
    }

    @Override
    public void update(DailyTask task) {
        baseMapper.updateById(task);
        log.info("更新每日任务成功: ID={}", task.getId());
    }

    @Override
    public void complete(Long id, Integer completed) {
        DailyTask task = new DailyTask();
        task.setId(id);
        task.setCompleted(completed);
        baseMapper.updateById(task);
        log.info("每日任务状态更新成功: ID={}, completed={}", id, completed);
    }

    @Override
    public void delete(Long id) {
        baseMapper.deleteById(id);
        log.info("删除每日任务成功: ID={}", id);
    }
}
