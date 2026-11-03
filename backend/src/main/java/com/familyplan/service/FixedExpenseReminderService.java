package com.familyplan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familyplan.entity.FamilyMember;
import com.familyplan.entity.FixedExpense;
import com.familyplan.entity.Notification;
import com.familyplan.mapper.NotificationMapper;
import com.familyplan.support.FixedExpenseReminderPlanner;
import com.familyplan.support.FixedExpenseReminderPlanner.ReminderDraft;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 把启用中的固定支出缴费日写成提醒中心通知。
 * 同一家庭、同一账单、同一账期只保留一条：biz_key = FE:{账单ID}:{yyyyMM}。
 * 已读或已删除的不再新建。未读的会跟着到期状态改文案；缴费日挪出窗口后，未读提醒会删掉。
 */
@Slf4j
@Service
public class FixedExpenseReminderService {

    @Autowired
    private IFixedExpenseService fixedExpenseService;

    @Autowired
    private IFamilyMemberService memberService;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${familyplan.reminder.lead-days:3}")
    private int leadDays;

    @Value("${familyplan.reminder.zone:Asia/Shanghai}")
    private String zoneId;

    private final ConcurrentHashMap<Long, Object> locks = new ConcurrentHashMap<>();

    /**
     * 当前家庭：打开提醒列表或未读数时调用。
     */
    public void generateForFamily(Long familyId) {
        if (familyId == null) {
            return;
        }
        List<Long> memberIds = memberService.getListByFamilyId(familyId).stream()
                .map(FamilyMember::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (memberIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<FixedExpense> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FixedExpense::getMemberId, memberIds);
        List<FixedExpense> expenses = fixedExpenseService.list(wrapper);
        LocalDate today = today();
        synchronized (lock(familyId)) {
            for (FixedExpense expense : expenses) {
                syncOne(familyId, expense, today);
            }
        }
    }

    /**
     * 全部家庭：每天定时任务调用。
     */
    public void generateAll() {
        List<FixedExpense> expenses = fixedExpenseService.list();
        if (expenses.isEmpty()) {
            return;
        }
        Set<Long> memberIds = expenses.stream()
                .map(FixedExpense::getMemberId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (memberIds.isEmpty()) {
            return;
        }
        Map<Long, Long> familyByMember = memberService.listByIds(memberIds).stream()
                .filter(member -> member.getFamilyId() != null)
                .collect(Collectors.toMap(FamilyMember::getId, FamilyMember::getFamilyId, (a, b) -> a));
        Map<Long, List<FixedExpense>> byFamily = new HashMap<>();
        for (FixedExpense expense : expenses) {
            Long familyId = familyByMember.get(expense.getMemberId());
            if (familyId == null) {
                continue;
            }
            byFamily.computeIfAbsent(familyId, key -> new ArrayList<>()).add(expense);
        }
        LocalDate today = today();
        int families = 0;
        for (Map.Entry<Long, List<FixedExpense>> entry : byFamily.entrySet()) {
            synchronized (lock(entry.getKey())) {
                for (FixedExpense expense : entry.getValue()) {
                    syncOne(entry.getKey(), expense, today);
                }
            }
            families++;
        }
        log.info("固定支出缴费提醒扫描完成: 家庭={}", families);
    }

    private void syncOne(Long familyId, FixedExpense expense, LocalDate today) {
        try {
            if (expense.getId() == null) {
                return;
            }
            boolean active = expense.getActive() != null && expense.getActive() == 1
                    && expense.getDueDay() != null && expense.getDueDay() >= 1;
            if (!active) {
                clearWindow(familyId, expense.getId(), today);
                return;
            }
            List<ReminderDraft> drafts = FixedExpenseReminderPlanner.plan(
                    today, leadDays, expense.getId(), expense.getExpenseName(),
                    expense.getAmount(), expense.getDueDay());
            Set<String> desired = new HashSet<>();
            for (ReminderDraft draft : drafts) {
                desired.add(draft.bizKey());
                upsert(familyId, draft);
            }
            YearMonth current = YearMonth.from(today);
            for (YearMonth period : List.of(current, current.plusMonths(1))) {
                String key = FixedExpenseReminderPlanner.bizKey(expense.getId(), period);
                if (!desired.contains(key)) {
                    removeUnread(familyId, key);
                }
            }
        } catch (RuntimeException ex) {
            log.warn("固定支出提醒生成失败 expenseId={}: {}", expense.getId(), ex.getMessage());
        }
    }

    private void clearWindow(Long familyId, Long expenseId, LocalDate today) {
        YearMonth current = YearMonth.from(today);
        removeUnread(familyId, FixedExpenseReminderPlanner.bizKey(expenseId, current));
        removeUnread(familyId, FixedExpenseReminderPlanner.bizKey(expenseId, current.plusMonths(1)));
    }

    private void upsert(Long familyId, ReminderDraft draft) {
        Notification existing = findVisible(familyId, draft.bizKey());
        if (existing != null) {
            if (existing.getIsRead() != null && existing.getIsRead() == 1) {
                return;
            }
            if (!draft.title().equals(existing.getTitle()) || !draft.content().equals(existing.getContent())) {
                existing.setTitle(draft.title());
                existing.setContent(draft.content());
                existing.setType("BILL");
                notificationMapper.updateById(existing);
            }
            return;
        }
        if (countAny(familyId, draft.bizKey()) > 0) {
            return;
        }
        Notification created = new Notification();
        created.setFamilyId(familyId);
        created.setTitle(draft.title());
        created.setContent(draft.content());
        created.setType("BILL");
        created.setIsRead(0);
        created.setBizKey(draft.bizKey());
        try {
            notificationMapper.insert(created);
            log.info("已生成固定支出提醒: familyId={}, key={}", familyId, draft.bizKey());
        } catch (RuntimeException ex) {
            if (!isDuplicate(ex)) {
                throw ex;
            }
            log.debug("固定支出提醒已存在: {}", draft.bizKey());
        }
    }

    private void removeUnread(Long familyId, String bizKey) {
        Notification existing = findVisible(familyId, bizKey);
        if (existing != null && (existing.getIsRead() == null || existing.getIsRead() == 0)) {
            notificationMapper.deleteById(existing.getId());
        }
    }

    private Notification findVisible(Long familyId, String bizKey) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getFamilyId, familyId)
                .eq(Notification::getBizKey, bizKey)
                .last("LIMIT 1");
        return notificationMapper.selectOne(wrapper);
    }

    /**
     * 含已逻辑删除的行。删过的提醒不再生成，避免反复打扰。
     */
    private int countAny(Long familyId, String bizKey) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE family_id = ? AND biz_key = ?",
                Integer.class, familyId, bizKey);
        return count == null ? 0 : count;
    }

    private boolean isDuplicate(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof java.sql.SQLIntegrityConstraintViolationException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && (message.contains("Duplicate") || message.contains("uk_notification_biz"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private LocalDate today() {
        try {
            return LocalDate.now(ZoneId.of(zoneId));
        } catch (DateTimeException ex) {
            return LocalDate.now(ZoneId.of("Asia/Shanghai"));
        }
    }

    private Object lock(Long familyId) {
        return locks.computeIfAbsent(familyId, key -> new Object());
    }
}
