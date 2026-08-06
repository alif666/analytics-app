package com.alif.analytics.meter.controller;

import com.alif.analytics.meter.dto.MeterAnalyticsDto;
import com.alif.analytics.meter.service.MeterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/meter")
@RequiredArgsConstructor
public class MeterController {
    private final MeterService meterService;

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public java.util.Map<String, String> validate(@RequestPart("file") MultipartFile file,
                                                  org.springframework.security.core.Authentication authentication) {
        meterService.validateFile(file, authentication.getName());
        return java.util.Map.of("message", "CSV format and data are valid");
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MeterAnalyticsDto upload(@RequestPart("file") MultipartFile file,
                                    @RequestParam BigDecimal dailyBudget,
                                    @RequestParam BigDecimal ratePerKwh,
                                    org.springframework.security.core.Authentication authentication) {
        return meterService.upload(file, dailyBudget, ratePerKwh, authentication.getName());
    }

    @GetMapping("/latest")
    public MeterAnalyticsDto latest(@RequestParam BigDecimal dailyBudget,
                                    @RequestParam BigDecimal ratePerKwh,
                                    org.springframework.security.core.Authentication authentication) {
        return meterService.latest(dailyBudget, ratePerKwh, authentication.getName());
    }
}
