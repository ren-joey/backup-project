package com.delta.dms.community.controller;

import com.delta.dms.community.service.ReportService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(
        value = "Report",
        tags = {"Report"},
        description = "Generate Excel Report"
)
@RestController
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping("/generateDSMPReport")
    public ResponseEntity<InputStreamResource> generateActivityLogReport(
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        return reportService.generateDSMPReportExcel(startTime, endTime);
    }
}