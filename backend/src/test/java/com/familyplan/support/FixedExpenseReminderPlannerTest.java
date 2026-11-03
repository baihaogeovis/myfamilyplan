package com.familyplan.support;

import com.familyplan.support.FixedExpenseReminderPlanner.ReminderDraft;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedExpenseReminderPlannerTest {

    @Test
    void remindsThreeDaysBeforeAndOnTheDueDate() {
        List<ReminderDraft> upcoming = FixedExpenseReminderPlanner.plan(
                LocalDate.of(2026, 11, 7), 3, 9L, "物业费", new BigDecimal("320"), 10);
        assertEquals(1, upcoming.size());
        assertEquals("UPCOMING", upcoming.get(0).phase());
        assertEquals("FE:9:202611", upcoming.get(0).bizKey());
        assertTrue(upcoming.get(0).title().startsWith("即将缴费"));

        List<ReminderDraft> due = FixedExpenseReminderPlanner.plan(
                LocalDate.of(2026, 11, 10), 3, 9L, "物业费", new BigDecimal("320"), 10);
        assertEquals("DUE", due.get(0).phase());
        assertTrue(due.get(0).title().startsWith("今天缴费"));
        assertTrue(due.get(0).content().contains("¥320.00"));
    }

    @Test
    void staysQuietOutsideTheLeadWindow() {
        List<ReminderDraft> drafts = FixedExpenseReminderPlanner.plan(
                LocalDate.of(2026, 11, 6), 3, 9L, "物业费", new BigDecimal("320"), 10);
        assertTrue(drafts.isEmpty());
    }

    @Test
    void marksCurrentMonthOverdueAndNextMonthWhenItEntersTheWindow() {
        List<ReminderDraft> drafts = FixedExpenseReminderPlanner.plan(
                LocalDate.of(2026, 11, 30), 3, 9L, "话费", new BigDecimal("80"), 2);
        assertEquals(2, drafts.size());
        assertEquals("OVERDUE", drafts.get(0).phase());
        assertEquals("FE:9:202611", drafts.get(0).bizKey());
        assertTrue(drafts.get(0).content().contains("逾期"));
        assertEquals("UPCOMING", drafts.get(1).phase());
        assertEquals("FE:9:202612", drafts.get(1).bizKey());
    }

    @Test
    void clampsDueDayToTheEndOfAShortMonth() {
        assertEquals(LocalDate.of(2026, 2, 28),
                FixedExpenseReminderPlanner.dueDate(YearMonth.of(2026, 2), 31));
        assertEquals(LocalDate.of(2028, 2, 29),
                FixedExpenseReminderPlanner.dueDate(YearMonth.of(2028, 2), 31));
        List<ReminderDraft> due = FixedExpenseReminderPlanner.plan(
                LocalDate.of(2026, 2, 28), 3, 4L, "网费", null, 31);
        assertEquals(1, due.size());
        assertEquals("DUE", due.get(0).phase());
        assertEquals("FE:4:202602", due.get(0).bizKey());
        List<ReminderDraft> upcoming = FixedExpenseReminderPlanner.plan(
                LocalDate.of(2026, 2, 27), 3, 4L, "网费", null, 31);
        assertEquals("UPCOMING", upcoming.get(0).phase());
        assertTrue(upcoming.get(0).content().contains("2月28日"));
    }

    @Test
    void skipsBillsWithoutADueDay() {
        assertTrue(FixedExpenseReminderPlanner.plan(
                LocalDate.of(2026, 11, 3), 3, 1L, "其他", BigDecimal.TEN, null).isEmpty());
    }
}
