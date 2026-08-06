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

@Service
@RequiredArgsConstructor
public class MeterService {
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("d-MMM-uuuu", Locale.ENGLISH);
    private final MeterImportRepository importRepository;

    @Value("${analytics.meter.upload-directory:uploads/meter}")
    private String uploadDirectory;

    @Transactional
    public MeterAnalyticsDto upload(MultipartFile file, BigDecimal dailyBudget, BigDecimal ratePerKwh, String username) {
        validateFileType(file);
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

    public void validateFile(MultipartFile file) {
        validateFileType(file);
        parse(file);
    }

    @Transactional(readOnly = true)
    public MeterAnalyticsDto latest(BigDecimal dailyBudget, BigDecimal ratePerKwh, String username) {
        validateInputs(dailyBudget, ratePerKwh);
        MeterImport latest = importRepository.findTopByUploadedByOrderByUploadedAtDesc(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload a meter CSV to see analytics"));
        return toDto(latest, dailyBudget, ratePerKwh);
    }

    private static void validateFileType(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null ||
                !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please select a non-empty CSV file");
        }
    }

    private Map<LocalDate, BigDecimal> parse(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new StringReader(new String(file.getBytes(), StandardCharsets.UTF_8)))) {
            String line;
            int lineNumber = 0;
            int dateColumn = -1, usageColumn = -1, unitColumn = -1;
            boolean headerFound = false;
            boolean summaryFound = false;
            BigDecimal summaryTotal = null;
            Map<LocalDate, BigDecimal> result = new TreeMap<>();
            List<String> errors = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                List<String> cells = csvCells(line);
                boolean blank = cells.stream().allMatch(String::isBlank);
                if (blank) continue;

                if (!headerFound) {
                    Map<String, Integer> headers = headerIndexes(cells);
                    if (headers.containsKey("date") || headers.containsKey("totalusage") || headers.containsKey("usageuom")) {
                        headerFound = true;
                        dateColumn = headers.getOrDefault("date", -1);
                        usageColumn = headers.getOrDefault("totalusage", headers.getOrDefault("usage", -1));
                        unitColumn = headers.getOrDefault("usageuom", -1);
                        if (dateColumn < 0) errors.add("Row " + lineNumber + ": required column 'Date' is missing");
                        if (usageColumn < 0) errors.add("Row " + lineNumber + ": required column 'Total Usage' is missing");
                        if (unitColumn < 0) errors.add("Row " + lineNumber + ": required column 'Usage UOM' is missing");
                    }
                    continue;
                }

                if (summaryFound) {
                    errors.add("Row " + lineNumber + ": data appears after the 'Total' summary row");
                    continue;
                }
                if (cells.size() <= Math.max(dateColumn, Math.max(usageColumn, unitColumn))) {
                    errors.add("Row " + lineNumber + ": expected Date, Total Usage, and Usage UOM values");
                    continue;
                }

                String dateValue = cells.get(dateColumn).trim();
                String usageValue = cells.get(usageColumn).trim().replace(",", "");
                String unitValue = cells.get(unitColumn).trim();
                if (normalize(dateValue).equals("total")) {
                    summaryFound = true;
                    try {
                        summaryTotal = new BigDecimal(usageValue);
                        if (summaryTotal.signum() < 0) errors.add("Row " + lineNumber + ": total usage cannot be negative");
                    } catch (NumberFormatException ex) {
                        errors.add("Row " + lineNumber + ": total usage must be a number");
                    }
                    if (!normalizeUnit(unitValue).equals("kwh")) errors.add("Row " + lineNumber + ": Usage UOM must be kWh");
                    continue;
                }

                LocalDate date;
                try {
                    date = parseDate(dateValue);
                } catch (DateTimeParseException ex) {
                    errors.add("Row " + lineNumber + ": Date must use YYYY-MM-DD, DD/MM/YYYY, or DD-MMM-YYYY format");
                    continue;
                }
                BigDecimal usage;
                try {
                    usage = new BigDecimal(usageValue);
                } catch (NumberFormatException ex) {
                    errors.add("Row " + lineNumber + ": Total Usage must be a number");
                    continue;
                }
                if (usage.signum() < 0) errors.add("Row " + lineNumber + ": Total Usage cannot be negative");
                if (!normalizeUnit(unitValue).equals("kwh")) errors.add("Row " + lineNumber + ": Usage UOM must be kWh");
                if (result.containsKey(date)) errors.add("Row " + lineNumber + ": duplicate reading date " + dateValue);
                else if (usage.signum() >= 0) result.put(date, usage);
            }

            if (!headerFound) errors.add("No meter table found. Expected a header row with Date, Total Usage, and Usage UOM");
            if (result.isEmpty()) errors.add("At least one valid daily reading is required");
            if (summaryTotal != null) {
                BigDecimal readingTotal = result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                if (summaryTotal.subtract(readingTotal).abs().compareTo(new BigDecimal("0.01")) > 0) {
                    errors.add("The Total summary does not match the sum of daily readings");
                }
            }
            if (!errors.isEmpty()) throw validationError(errors);
            return result;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the CSV file", ex);
        }
    }

    private static Map<String, Integer> headerIndexes(List<String> cells) {
        Map<String, Integer> headers = new HashMap<>();
        for (int i = 0; i < cells.size(); i++) {
            String header = normalize(cells.get(i));
            if (header.equals("date") || header.equals("readingdate")) headers.put("date", i);
            if (header.equals("totalusage") || header.startsWith("totalusage")) headers.put("totalusage", i);
            if (header.equals("usage") || header.equals("consumption")) headers.put("usage", i);
            if (header.equals("usageuom") || header.equals("unit")) headers.put("usageuom", i);
        }
        return headers;
    }

    private static ResponseStatusException validationError(List<String> errors) {
        String detail = String.join(" | ", errors.size() > 12 ? errors.subList(0, 12) : errors);
        if (errors.size() > 12) detail += " | Additional errors were omitted";
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meter CSV validation failed: " + detail);
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
    private static String normalize(String value) { return value.replace("\uFEFF", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); }
    private static String normalizeUnit(String value) { return value.replace("\uFEFF", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); }
    private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private static BigDecimal scale(BigDecimal value) { return value.setScale(3, RoundingMode.HALF_UP); }
    private static void validateInputs(BigDecimal budget, BigDecimal rate) { if (budget == null || rate == null || budget.signum() <= 0 || rate.signum() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Daily budget and rate per kWh must be greater than zero"); }
}
