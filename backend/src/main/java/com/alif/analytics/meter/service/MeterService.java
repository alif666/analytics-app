package com.alif.analytics.meter.service;

import com.alif.analytics.meter.dto.MeterAnalyticsDto;
import com.alif.analytics.meter.entity.MeterImport;
import com.alif.analytics.meter.entity.MeterReading;
import com.alif.analytics.meter.repository.MeterImportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeterService {
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("d-MMM-uuuu", Locale.ENGLISH);
    private final MeterImportRepository importRepository;

    @Value("${analytics.meter.upload-directory:uploads/meter}")
    private String uploadDirectory;

    @Transactional
    public MeterAnalyticsDto upload(MultipartFile file, BigDecimal dailyBudget, BigDecimal ratePerKwh, String username) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null ||
                !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload a non-empty CSV file");
        }
        validateInputs(dailyBudget, ratePerKwh);
        Map<LocalDate, BigDecimal> parsed = parse(file);
        try {
            Path root = Paths.get(uploadDirectory).toAbsolutePath().normalize();
            Files.createDirectories(root);
            String safeName = Paths.get(file.getOriginalFilename()).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
            Path destination = root.resolve(UUID.randomUUID() + "-" + safeName).normalize();
            if (!destination.startsWith(root)) throw new IOException("Invalid upload path");
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            MeterImport meterImport = new MeterImport();
            meterImport.setOriginalFilename(file.getOriginalFilename());
            meterImport.setStoredPath(destination.toString());
            meterImport.setUploadedBy(username);
            parsed.forEach((date, usage) -> {
                MeterReading reading = new MeterReading();
                reading.setMeterImport(meterImport);
                reading.setReadingDate(date);
                reading.setConsumptionKwh(usage);
                meterImport.getReadings().add(reading);
            });
            return toDto(importRepository.save(meterImport), dailyBudget, ratePerKwh);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the uploaded CSV", ex);
        }
    }

    @Transactional(readOnly = true)
    public MeterAnalyticsDto latest(BigDecimal dailyBudget, BigDecimal ratePerKwh, String username) {
        validateInputs(dailyBudget, ratePerKwh);
        MeterImport latest = importRepository.findTopByUploadedByOrderByUploadedAtDesc(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload a meter CSV to see analytics"));
        return toDto(latest, dailyBudget, ratePerKwh);
    }

    private Map<LocalDate, BigDecimal> parse(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new StringReader(new String(file.getBytes(), StandardCharsets.UTF_8)))) {
            String line;
            int dateColumn = -1, usageColumn = -1;
            Map<LocalDate, BigDecimal> result = new TreeMap<>();
            while ((line = reader.readLine()) != null) {
                List<String> cells = csvCells(line);
                if (dateColumn < 0) {
                    for (int i = 0; i < cells.size(); i++) {
                        String header = normalize(cells.get(i));
                        if (header.equals("date") || header.equals("reading date")) dateColumn = i;
                        if (header.contains("totalusage") || header.equals("usage") || header.contains("consumption")) usageColumn = i;
                    }
                    if (dateColumn < 0 || usageColumn < 0) continue;
                    continue;
                }
                if (cells.size() <= Math.max(dateColumn, usageColumn)) continue;
                try {
                    LocalDate date = parseDate(cells.get(dateColumn));
                    BigDecimal usage = new BigDecimal(cells.get(usageColumn).replace(",", "").trim());
                    if (usage.signum() >= 0) result.merge(date, usage, BigDecimal::add);
                } catch (DateTimeParseException | NumberFormatException ignored) {
                    // Metadata and summary rows are not meter readings.
                }
            }
            if (result.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV must contain Date and Usage columns with at least one reading");
            return result;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the CSV file", ex);
        }
    }

    private MeterAnalyticsDto toDto(MeterImport meterImport, BigDecimal budget, BigDecimal rate) {
        List<MeterAnalyticsDto.DailyReadingDto> rows = meterImport.getReadings().stream()
                .sorted(Comparator.comparing(MeterReading::getReadingDate))
                .map(r -> new MeterAnalyticsDto.DailyReadingDto(r.getReadingDate(), r.getConsumptionKwh(), money(r.getConsumptionKwh().multiply(rate)), r.getConsumptionKwh().multiply(rate).compareTo(budget) > 0))
                .toList();
        BigDecimal total = rows.stream().map(MeterAnalyticsDto.DailyReadingDto::consumptionKwh).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = total.divide(BigDecimal.valueOf(rows.size()), 3, RoundingMode.HALF_UP);
        var peak = rows.stream().max(Comparator.comparing(MeterAnalyticsDto.DailyReadingDto::consumptionKwh)).orElseThrow();
        BigDecimal actualCost = money(average.multiply(rate));
        return new MeterAnalyticsDto(meterImport.getId(), meterImport.getOriginalFilename(), rows.size(), rows.get(0).date(), rows.get(rows.size() - 1).date(), scale(total), average, peak.consumptionKwh(), peak.date(), rows.stream().map(MeterAnalyticsDto.DailyReadingDto::consumptionKwh).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO), money(budget), money(rate), money(budget.divide(rate, 3, RoundingMode.HALF_UP)), actualCost, money(actualCost.multiply(BigDecimal.valueOf(rows.size()))), money(budget.subtract(actualCost)), rows);
    }

    private static LocalDate parseDate(String value) {
        String text = value.trim();
        for (DateTimeFormatter formatter : List.of(DateTimeFormatter.ISO_LOCAL_DATE, DISPLAY_DATE, DateTimeFormatter.ofPattern("dd/MM/uuuu"))) {
            try { return LocalDate.parse(text, formatter); } catch (DateTimeParseException ignored) { }
        }
        throw new DateTimeParseException("Invalid date", text, 0);
    }
    private static List<String> csvCells(String line) { List<String> cells = new ArrayList<>(); StringBuilder cell = new StringBuilder(); boolean quoted = false; for (int i=0;i<line.length();i++) { char c=line.charAt(i); if(c=='"') { if(quoted && i+1<line.length() && line.charAt(i+1)=='"'){cell.append('"');i++;} else quoted=!quoted; } else if(c==',' && !quoted){cells.add(cell.toString());cell.setLength(0);} else cell.append(c); } cells.add(cell.toString()); return cells; }
    private static String normalize(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "").replace("kwh", ""); }
    private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private static BigDecimal scale(BigDecimal value) { return value.setScale(3, RoundingMode.HALF_UP); }
    private static void validateInputs(BigDecimal budget, BigDecimal rate) { if (budget == null || rate == null || budget.signum() <= 0 || rate.signum() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Daily budget and rate per kWh must be greater than zero"); }
}
