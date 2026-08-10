package com.alif.analytics.meter.controller;

import com.alif.analytics.meter.dto.MonthlyUsageDto;
import com.alif.analytics.meter.service.IMeterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/meter")
@RequiredArgsConstructor
public class MeterController {
    private final IMeterService meterService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<MonthlyUsageDto> analyze(@RequestPart("file") MultipartFile file,
                                         @RequestParam(defaultValue = "7") BigDecimal sanctionedLoad) {
        return meterService.analyze(file, sanctionedLoad);
    }

    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MonthlyUsageDto save(@RequestPart("file") MultipartFile file,
                                @RequestParam int month, @RequestParam int year,
                                @RequestParam(defaultValue = "7") BigDecimal sanctionedLoad,
                                Authentication authentication) {
        return meterService.save(file, month, year, sanctionedLoad, authentication.getName());
    }

    @GetMapping("/monthly")
    public List<MonthlyUsageDto> monthly(Authentication authentication) {
        return meterService.monthly(authentication.getName());
    }
}
