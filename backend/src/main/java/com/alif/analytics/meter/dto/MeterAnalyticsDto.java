package com.alif.analytics.meter.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MeterAnalyticsDto(
        Long importId,
        String filename,
        int readingCount,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalKwh,
        BigDecimal averageDailyKwh,
        BigDecimal peakDailyKwh,
        LocalDate peakDate,
        BigDecimal minimumDailyKwh,
        BigDecimal dailyBudget,
        BigDecimal ratePerKwh,
        BigDecimal recommendedDailyKwh,
        BigDecimal actualAverageDailyCost,
        BigDecimal projectedPeriodCost,
        BigDecimal budgetVariancePerDay,
        List<DailyReadingDto> readings
) {
    public record DailyReadingDto(LocalDate date, BigDecimal consumptionKwh, BigDecimal cost, boolean overBudget) {}
}
