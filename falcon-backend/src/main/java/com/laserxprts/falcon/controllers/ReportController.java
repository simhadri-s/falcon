package com.laserxprts.falcon.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.dto.response.ReportDashboardResponse;
import com.laserxprts.falcon.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PreAuthorize("@permissionService.hasAccess('ALL_PERMISSIONS')")
    @GetMapping
    public ResponseEntity<ReportDashboardResponse> getDashboardReport(
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(reportService.getDashboardReport(period));
    }

    @PreAuthorize("@permissionService.hasAccess('ALL_PERMISSIONS')")
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadPdfReport(
            @RequestParam(defaultValue = "30d") String period) {
        ReportService.PdfReportDownload report = reportService.generatePdfReport(period);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + report.fileName() + "\"")
                .header("Content-Type", "application/pdf")
                .body(report.content());
    }
}
