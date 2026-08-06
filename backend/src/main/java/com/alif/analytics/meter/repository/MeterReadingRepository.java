package com.alif.analytics.meter.repository;

import com.alif.analytics.meter.entity.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
    @Query("select r.readingDate from MeterReading r " +
            "where r.meterImport.uploadedBy = :uploadedBy and r.readingDate in :dates")
    List<LocalDate> findExistingDates(@Param("uploadedBy") String uploadedBy,
                                      @Param("dates") Collection<LocalDate> dates);
}
