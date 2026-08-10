package com.alif.analytics.meter.repository;

import com.alif.analytics.meter.entity.MonthlyMeterUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MonthlyMeterUsageRepository extends JpaRepository<MonthlyMeterUsage, Long> {
    List<MonthlyMeterUsage> findByUserEmailOrderByYearDescMonthDesc(String userEmail);
    boolean existsByUserEmailAndMonthAndYear(String userEmail, Integer month, Integer year);
}
