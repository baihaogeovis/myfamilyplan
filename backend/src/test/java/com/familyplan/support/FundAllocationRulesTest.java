package com.familyplan.support;

import com.familyplan.common.BusinessException;
import com.familyplan.entity.FundAllocation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FundAllocationRulesTest {

    @Test
    void acceptsPercentageThatKeepsThePeriodWithinOneHundred() {
        FundAllocationRules.assertWithinHundred(new BigDecimal("60.00"), new BigDecimal("40.00"));
        FundAllocationRules.assertWithinHundred(new BigDecimal("99.99"), new BigDecimal("0.01"));
    }

    @Test
    void rejectsPercentageThatPushesThePeriodOverOneHundred() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> FundAllocationRules.assertWithinHundred(new BigDecimal("60.00"), new BigDecimal("50.00")));
        assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    void rejectsZeroTooManyDecimalsAndMissingMonth() {
        assertThrows(BusinessException.class, () -> FundAllocationRules.normalizePercentage(new BigDecimal("0")));
        assertThrows(BusinessException.class, () -> FundAllocationRules.normalizePercentage(new BigDecimal("10.555")));
        assertThrows(BusinessException.class, () -> FundAllocationRules.normalizePercentage(new BigDecimal("100.01")));
        assertThrows(BusinessException.class, () -> FundAllocationRules.periodKey("MONTH", 2026, null));
    }

    @Test
    void normalizesYearlyAllocationAndClearsMonth() {
        FundAllocation item = new FundAllocation();
        item.setCategory(" food ");
        item.setPeriod("year");
        item.setYear(2026);
        item.setMonth(4);
        item.setPercentage(new BigDecimal("33.3"));

        FundAllocationRules.normalize(item);

        assertEquals("food", item.getCategory());
        assertEquals("YEAR", item.getPeriod());
        assertEquals(2026, item.getYear());
        assertEquals(null, item.getMonth());
        assertEquals(new BigDecimal("33.30"), item.getPercentage());
    }
}
