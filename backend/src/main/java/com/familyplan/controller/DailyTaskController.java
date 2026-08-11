package com.familyplan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.familyplan.common.Result;
import com.familyplan.entity.DailyTask;
import com.familyplan.service.IDailyTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日任务控制器(家庭/工作打勾工作台)
 */
@Slf4j
@RestController
@RequestMapping("/daily-task")
public class DailyTaskController {

    @Autowired
    private IDailyTaskService dailyTaskService;

    /**
     * 按日期查询任务列表(打勾工作台)
     */
    @GetMapping("/list")
    public Result<List<DailyTask>> list(@RequestParam Long memberId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(dailyTaskService.listByDate(memberId, date));
    }

    /**
     * 分页查询历史任务
     */
    @GetMapping("/page")
    public Result<IPage<DailyTask>> page(@RequestParam Long memberId,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) String taskType,
                                         @RequestParam(required = false) Integer completed,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(dailyTaskService.pageQuery(memberId, page, size, taskType, completed, startDate, endDate));
    }

    /**
     * 添加任务
     */
    @PostMapping
    public Result<Void> add(@RequestBody DailyTask task) {
        dailyTaskService.add(task);
        return Result.success();
    }

    /**
     * 更新任务
     */
    @PutMapping
    public Result<Void> update(@RequestBody DailyTask task) {
        dailyTaskService.update(task);
        return Result.success();
    }

    /**
     * 打勾/取消完成
     */
    @PutMapping("/complete/{id}")
    public Result<Void> complete(@PathVariable Long id, @RequestParam Integer completed) {
        dailyTaskService.complete(id, completed);
        return Result.success();
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dailyTaskService.delete(id);
        return Result.success();
    }
}
