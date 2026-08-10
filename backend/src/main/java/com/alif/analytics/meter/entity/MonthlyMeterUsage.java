package com.alif.analytics.meter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "monthly_meter_usage",
        uniqueConstraints = @UniqueConstraint(name = "uk_monthly_meter_usage_user_month_year", columnNames = {"user_email", "month", "year"}),
        indexes = @Index(name = "idx_monthly_meter_usage_user", columnList = "user_email"))
public class MonthlyMeterUsage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;
    @Column(nullable = false) private Integer month;
    @Column(nullable = false) private Integer year;
    @Column(name = "total_usage", nullable = false, precision = 14, scale = 3)
    private BigDecimal totalUsage;
    @Column(name = "usage_uom", nullable = false, length = 20)
    private String usageUom;
    @Column(name = "energy_charge", nullable = false, precision = 14, scale = 2)
    private BigDecimal energyCharge;
    @Column(name = "demand_charge", nullable = false, precision = 14, scale = 2)
    private BigDecimal demandCharge;
    @Column(name = "meter_rent", nullable = false, precision = 14, scale = 2)
    private BigDecimal meterRent;
    @Column(name = "vat", nullable = false, precision = 14, scale = 2)
    private BigDecimal vat;
    @Column(name = "estimated_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedAmount;
    @Column(name = "saved_at", nullable = false)
    private Instant savedAt = Instant.now();
}
