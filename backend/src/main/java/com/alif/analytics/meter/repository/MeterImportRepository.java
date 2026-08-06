package com.alif.analytics.meter.repository;

import com.alif.analytics.meter.entity.MeterImport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeterImportRepository extends JpaRepository<MeterImport, Long> {
    Optional<MeterImport> findTopByUploadedByOrderByUploadedAtDesc(String uploadedBy);
}
