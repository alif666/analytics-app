package com.alif.analytics.meter.service;

import com.alif.analytics.meter.dto.MonthlyUsageDto;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface IMeterService {
    List<MonthlyUsageDto> analyze(MultipartFile file, BigDecimal sanctionedLoad);

    MonthlyUsageDto save(MultipartFile file, int month, int year,
                         BigDecimal sanctionedLoad, String userEmail);

    List<MonthlyUsageDto> monthly(String userEmail);
}
