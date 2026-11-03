package com.familyplan.controller;

import com.familyplan.service.ExcelExportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Excel 导出。新增接口，不影响原有收支、预算和统计 JSON 接口。
 */
@Slf4j
@RestController
@RequestMapping("/export")
public class ExportController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @Autowired
    private ExcelExportService excelExportService;

    @GetMapping("/income")
    public ResponseEntity<byte[]> income(@RequestParam Long memberId,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return xlsx(excelExportService.exportIncome(memberId, category, startDate, endDate), "收入记录.xlsx");
    }

    @GetMapping("/expense")
    public ResponseEntity<byte[]> expense(@RequestParam Long memberId,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(required = false) String type,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return xlsx(excelExportService.exportExpense(memberId, category, type, startDate, endDate), "支出记录.xlsx");
    }

    @GetMapping("/budget")
    public ResponseEntity<byte[]> budget(@RequestParam Long memberId,
                                         @RequestParam(required = false) Integer year,
                                         @RequestParam(required = false) Integer month,
                                         @RequestParam(required = false) String periodType) {
        return xlsx(excelExportService.exportBudget(memberId, year, month, periodType), "预算计划.xlsx");
    }

    @GetMapping("/statistics")
    public ResponseEntity<byte[]> statistics(@RequestParam(required = false) Integer year,
                                             @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return xlsx(excelExportService.exportStatistics(year, month), "收支统计.xlsx");
    }

    private ResponseEntity<byte[]> xlsx(byte[] body, String filename) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(body);
    }
}
