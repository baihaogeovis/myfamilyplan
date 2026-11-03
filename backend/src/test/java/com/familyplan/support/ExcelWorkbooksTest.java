package com.familyplan.support;

import com.familyplan.dto.CategoryItem;
import com.familyplan.dto.MonthlyStat;
import com.familyplan.entity.ExpenseRecord;
import com.familyplan.entity.IncomeRecord;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelWorkbooksTest {

    @Test
    void writesIncomeAndExpenseWorkbooks() throws Exception {
        IncomeRecord income = new IncomeRecord();
        income.setIncomeDate(LocalDate.of(2026, 11, 2));
        income.setCategory("SALARY");
        income.setSource("公司");
        income.setAmount(new BigDecimal("8000"));
        income.setRemark("工资");
        byte[] incomeBytes = ExcelWorkbooks.income(List.of(income), false);
        assertEquals('P', incomeBytes[0]);
        assertEquals('K', incomeBytes[1]);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(incomeBytes))) {
            assertEquals("收入", wb.getSheetAt(0).getSheetName());
            assertEquals("工资", wb.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
            assertEquals(8000D, wb.getSheetAt(0).getRow(1).getCell(3).getNumericCellValue());
        }

        ExpenseRecord expense = new ExpenseRecord();
        expense.setExpenseDate(LocalDate.of(2026, 11, 3));
        expense.setCategory("FOOD");
        expense.setType("NORMAL");
        expense.setAmount(new BigDecimal("36.5"));
        byte[] expenseBytes = ExcelWorkbooks.expense(List.of(expense), true);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(expenseBytes))) {
            assertEquals("餐饮", wb.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
            assertEquals("普通", wb.getSheetAt(0).getRow(1).getCell(2).getStringCellValue());
            assertTrue(wb.getSheetAt(0).getRow(2).getCell(0).getStringCellValue().contains("10000"));
        }
    }

    @Test
    void writesStatisticsWorkbookWithThreeSheets() throws Exception {
        byte[] bytes = ExcelWorkbooks.statistics(
                2026,
                "2026-11",
                List.of(new MonthlyStat(11, new BigDecimal("100"), new BigDecimal("40"))),
                List.of(new CategoryItem("SALARY", new BigDecimal("100"))),
                List.of(new CategoryItem("FOOD", new BigDecimal("40"))));
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(3, wb.getNumberOfSheets());
            assertEquals(60D, wb.getSheetAt(0).getRow(1).getCell(3).getNumericCellValue());
            assertEquals("工资", wb.getSheetAt(1).getRow(1).getCell(0).getStringCellValue());
            assertEquals("餐饮", wb.getSheetAt(2).getRow(1).getCell(0).getStringCellValue());
        }
    }
}
