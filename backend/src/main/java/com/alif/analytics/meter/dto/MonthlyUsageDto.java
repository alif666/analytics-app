package com.alif.analytics.meter.dto;

import java.math.BigDecimal;

public record MonthlyUsageDto(Long id, Integer month, Integer year, BigDecimal totalUsage, String usageUom,
                              BigDecimal energyCharge, BigDecimal demandCharge, BigDecimal meterRent,
                              BigDecimal vat, BigDecimal estimatedAmount, boolean saved) {}
