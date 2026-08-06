package com.alif.analytics.meter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "meter_readings", indexes = @Index(name = "idx_meter_reading_import_date", columnList = "meter_import_id,reading_date"))
public class MeterReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_import_id", nullable = false)
    private MeterImport meterImport;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @Column(name = "consumption_kwh", nullable = false, precision = 14, scale = 3)
    private BigDecimal consumptionKwh;
}
