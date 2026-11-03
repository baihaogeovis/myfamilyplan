package com.familyplan.support;

import com.familyplan.dto.CategoryItem;
import com.familyplan.dto.MonthlyStat;
import com.familyplan.entity.BudgetPlan;
import com.familyplan.entity.ExpenseRecord;
import com.familyplan.entity.IncomeRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * 用 Apache POI 生成 xlsx。金额写成数字，分类写成中文。
 */
public final class ExcelWorkbooks {

    public static final int MAX_ROWS = 10000;

    private ExcelWorkbooks() {
    }

    public static byte[] income(List<IncomeRecord> rows, boolean truncated) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("收入");
            Styles styles = new Styles(wb);
            writeHeader(sheet, styles, "日期", "分类", "来源", "金额", "备注");
            int r = 1;
            for (IncomeRecord row : rows) {
                Row line = sheet.createRow(r++);
                text(line, 0, row.getIncomeDate() == null ? "" : row.getIncomeDate().toString(), styles.text);
                text(line, 1, CategoryLabels.category(row.getCategory()), styles.text);
                text(line, 2, row.getSource(), styles.text);
                money(line, 3, row.getAmount(), styles.money);
                text(line, 4, row.getRemark(), styles.text);
            }
            if (truncated) {
                note(sheet, r, styles);
            }
            widths(sheet, 14, 14, 24, 14, 36);
            return bytes(wb);
        } catch (IOException e) {
            throw new IllegalStateException("导出收入 Excel 失败", e);
        }
    }

    public static byte[] expense(List<ExpenseRecord> rows, boolean truncated) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("支出");
            Styles styles = new Styles(wb);
            writeHeader(sheet, styles, "日期", "分类", "类型", "金额", "备注");
            int r = 1;
            for (ExpenseRecord row : rows) {
                Row line = sheet.createRow(r++);
                text(line, 0, row.getExpenseDate() == null ? "" : row.getExpenseDate().toString(), styles.text);
                text(line, 1, CategoryLabels.category(row.getCategory()), styles.text);
                text(line, 2, CategoryLabels.expenseType(row.getType()), styles.text);
                money(line, 3, row.getAmount(), styles.money);
                text(line, 4, row.getRemark(), styles.text);
            }
            if (truncated) {
                note(sheet, r, styles);
            }
            widths(sheet, 14, 14, 12, 14, 36);
            return bytes(wb);
        } catch (IOException e) {
            throw new IllegalStateException("导出支出 Excel 失败", e);
        }
    }

    public static byte[] budget(List<BudgetPlan> rows, boolean truncated) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("预算");
            Styles styles = new Styles(wb);
            writeHeader(sheet, styles, "分类", "周期", "年份", "月份", "金额");
            int r = 1;
            for (BudgetPlan row : rows) {
                Row line = sheet.createRow(r++);
                text(line, 0, CategoryLabels.category(row.getCategory()), styles.text);
                text(line, 1, CategoryLabels.period(row.getPeriodType()), styles.text);
                text(line, 2, row.getYear() == null ? "" : String.valueOf(row.getYear()), styles.text);
                text(line, 3, row.getMonth() == null ? "" : String.valueOf(row.getMonth()), styles.text);
                money(line, 4, row.getAmount(), styles.money);
            }
            if (truncated) {
                note(sheet, r, styles);
            }
            widths(sheet, 14, 12, 10, 10, 14);
            return bytes(wb);
        } catch (IOException e) {
            throw new IllegalStateException("导出预算 Excel 失败", e);
        }
    }

    public static byte[] statistics(int year, String monthLabel, List<MonthlyStat> monthly,
                                    List<CategoryItem> income, List<CategoryItem> expense) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles styles = new Styles(wb);
            Sheet trend = wb.createSheet(year + "年月度收支");
            writeHeader(trend, styles, "月份", "收入", "支出", "结余");
            int r = 1;
            for (MonthlyStat stat : monthly) {
                Row line = trend.createRow(r++);
                text(line, 0, stat.getMonth() + "月", styles.text);
                money(line, 1, stat.getIncome(), styles.money);
                money(line, 2, stat.getExpense(), styles.money);
                BigDecimal incomeAmount = stat.getIncome() == null ? BigDecimal.ZERO : stat.getIncome();
                BigDecimal expenseAmount = stat.getExpense() == null ? BigDecimal.ZERO : stat.getExpense();
                money(line, 3, incomeAmount.subtract(expenseAmount), styles.money);
            }
            widths(trend, 12, 16, 16, 16);

            writeCategorySheet(wb, styles, monthLabel + "收入分类", income);
            writeCategorySheet(wb, styles, monthLabel + "支出分类", expense);
            return bytes(wb);
        } catch (IOException e) {
            throw new IllegalStateException("导出统计 Excel 失败", e);
        }
    }

    private static void writeCategorySheet(XSSFWorkbook wb, Styles styles, String name, List<CategoryItem> items) {
        String sheetName = name.length() > 31 ? name.substring(0, 31) : name;
        Sheet sheet = wb.createSheet(sheetName);
        writeHeader(sheet, styles, "分类", "金额");
        int r = 1;
        if (items != null) {
            for (CategoryItem item : items) {
                Row line = sheet.createRow(r++);
                text(line, 0, CategoryLabels.category(item.getCategory()), styles.text);
                money(line, 1, item.getAmount(), styles.money);
            }
        }
        widths(sheet, 18, 16);
    }

    private static void writeHeader(Sheet sheet, Styles styles, String... titles) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(titles[i]);
            cell.setCellStyle(styles.header);
        }
        sheet.createFreezePane(0, 1);
    }

    private static void text(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void money(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? 0D : value.doubleValue());
        cell.setCellStyle(style);
    }

    private static void note(Sheet sheet, int rowIndex, Styles styles) {
        Row row = sheet.createRow(rowIndex);
        text(row, 0, "仅导出前" + MAX_ROWS + "条，请缩小筛选范围后再导出", styles.text);
    }

    private static void widths(Sheet sheet, int... chars) {
        for (int i = 0; i < chars.length; i++) {
            sheet.setColumnWidth(i, chars[i] * 256);
        }
    }

    private static byte[] bytes(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private static final class Styles {
        final CellStyle header;
        final CellStyle text;
        final CellStyle money;

        Styles(XSSFWorkbook wb) {
            header = wb.createCellStyle();
            header.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xE8, (byte) 0x5D, (byte) 0x4C}, null));
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            Font font = wb.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            header.setFont(font);

            text = wb.createCellStyle();

            money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        }
    }
}
