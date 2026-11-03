package com.villagemart.dashboard;

import com.villagemart.common.BusinessClock;
import com.villagemart.dashboard.DashboardDtos.DashboardSummaryDto;
import com.villagemart.report.DateRange;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Dashboard")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;
    private final BusinessClock clock;

    @Operation(summary = "Dashboard figures for a period (defaults to today in the business timezone)")
    @GetMapping("/summary")
    public DashboardSummaryDto summary(@RequestParam(required = false) String period,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(DateRange.resolve(period, from, to, clock.today()));
    }
}
