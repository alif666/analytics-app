package com.alif.analytics.meter.service.impl;

import com.alif.analytics.meter.dto.MonthlyUsageDto;
import com.alif.analytics.meter.entity.MonthlyMeterUsage;
import com.alif.analytics.meter.repository.MonthlyMeterUsageRepository;
import com.alif.analytics.meter.service.IMeterService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.time.Month;
import java.util.*;

@Service @RequiredArgsConstructor
public class MeterServiceImpl implements IMeterService {
    private static final BigDecimal VAT_RATE = new BigDecimal("0.05");
    private static final BigDecimal DEMAND_RATE = new BigDecimal("42");
    private static final BigDecimal METER_RENT = new BigDecimal("40");
    private final MonthlyMeterUsageRepository repository;

    @Override
    public List<MonthlyUsageDto> analyze(MultipartFile file, BigDecimal sanctionedLoad) {
        return parse(file).stream().map(row -> calculate(row, sanctionedLoad, null)).toList();
    }

    @Override
    @Transactional
    public MonthlyUsageDto save(MultipartFile file, int month, int year, BigDecimal sanctionedLoad, String userEmail) {
        ParsedRow row = parse(file).stream().filter(r -> r.month == month && r.year == year).findFirst()
                .orElseThrow(() -> bad("The selected month and year are not present in the CSV"));
        if (repository.existsByUserEmailAndMonthAndYear(userEmail, month, year))
            throw bad("This user already has a saved entry for " + Month.of(month) + " " + year);
        MonthlyUsageDto calculated = calculate(row, sanctionedLoad, null);
        MonthlyMeterUsage entity = new MonthlyMeterUsage();
        entity.setUserEmail(userEmail); entity.setMonth(month); entity.setYear(year);
        entity.setTotalUsage(calculated.totalUsage()); entity.setUsageUom(calculated.usageUom());
        entity.setEnergyCharge(calculated.energyCharge()); entity.setDemandCharge(calculated.demandCharge());
        entity.setMeterRent(calculated.meterRent()); entity.setVat(calculated.vat());
        entity.setEstimatedAmount(calculated.estimatedAmount());
        try { return calculate(row, sanctionedLoad, repository.saveAndFlush(entity).getId()); }
        catch (DataIntegrityViolationException ex) { throw bad("This user already has a saved entry for this month and year"); }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyUsageDto> monthly(String userEmail) {
        return repository.findByUserEmailOrderByYearDescMonthDesc(userEmail).stream()
                .map(e -> new MonthlyUsageDto(e.getId(), e.getMonth(), e.getYear(), e.getTotalUsage(), e.getUsageUom(), e.getEnergyCharge(), e.getDemandCharge(), e.getMeterRent(), e.getVat(), e.getEstimatedAmount(), true)).toList();
    }

    private MonthlyUsageDto calculate(ParsedRow row, BigDecimal sanctionedLoad, Long id) {
        if (sanctionedLoad == null || sanctionedLoad.signum() <= 0) throw bad("Sanctioned load must be greater than zero");
        BigDecimal remaining = row.usage, energy = BigDecimal.ZERO;
        BigDecimal[] limits = {new BigDecimal("75"), new BigDecimal("125"), new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("200")};
        BigDecimal[] rates = {new BigDecimal("5.26"), new BigDecimal("8.50"), new BigDecimal("9.10"), new BigDecimal("9.62"), new BigDecimal("15.01")};
        for (int i = 0; i < limits.length; i++) { BigDecimal used = remaining.min(limits[i]).max(BigDecimal.ZERO); energy = energy.add(used.multiply(rates[i])); remaining = remaining.subtract(used); }
        if (remaining.signum() > 0) energy = energy.add(remaining.multiply(new BigDecimal("17.35")));
        energy = money(energy);
        BigDecimal demand = money(sanctionedLoad.multiply(DEMAND_RATE));
        BigDecimal vat = money(energy.add(demand).add(METER_RENT).multiply(VAT_RATE));
        BigDecimal total = money(energy.add(demand).add(METER_RENT).add(vat));
        return new MonthlyUsageDto(id, row.month, row.year, row.usage.setScale(3, RoundingMode.HALF_UP), "kWh", energy, demand, METER_RENT, vat, total, id != null);
    }

    private List<ParsedRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".csv")) throw bad("Please select a non-empty monthly CSV file");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line; int lineNo = 0; int monthCol = -1, yearCol = -1, usageCol = -1, uomCol = -1; boolean header = false; List<ParsedRow> rows = new ArrayList<>();
            while ((line = reader.readLine()) != null) { lineNo++; List<String> c = cells(line); if (c.stream().allMatch(String::isBlank)) continue;
                if (!header) { Map<String,Integer> h = headers(c); if (h.containsKey("month") || h.containsKey("year") || h.containsKey("totalusage")) { header = true; monthCol=h.getOrDefault("month",-1); yearCol=h.getOrDefault("year",-1); usageCol=h.getOrDefault("totalusage",-1); uomCol=h.getOrDefault("usageuom",-1); } continue; }
                int max = Math.max(Math.max(monthCol, yearCol), Math.max(usageCol, uomCol)); if (monthCol < 0 || usageCol < 0 || uomCol < 0 || c.size() <= max) throw bad("Row " + lineNo + ": expected Month, Year, Total Usage, and Usage UOM");
                if (normalize(c.get(monthCol)).equals("total")) continue;
                try { int month; int year; if (yearCol >= 0) { month=parseMonth(c.get(monthCol)); year=Integer.parseInt(c.get(yearCol).trim()); } else { int[] monthYear=parseMonthYear(c.get(monthCol)); month=monthYear[0]; year=monthYear[1]; } BigDecimal usage=new BigDecimal(c.get(usageCol).trim().replace(",", "")); String uom=c.get(uomCol).trim(); if (month<1||month>12||year<2000||usage.signum()<0||!normalize(uom).equals("kwh")) throw new IllegalArgumentException(); rows.add(new ParsedRow(month,year,usage)); } catch (Exception ex) { throw bad("Row " + lineNo + ": Month, Year, Total Usage, and Usage UOM are invalid"); }
            }
            if (!header || rows.isEmpty()) throw bad("No monthly usage table found. Expected Month, Year, Total Usage, and Usage UOM");
            Set<String> seen = new HashSet<>(); for (ParsedRow r: rows) if (!seen.add(r.year+"-"+r.month)) throw bad("Duplicate month/year in CSV: " + r.month + "/" + r.year);
            return rows;
        } catch (IOException ex) { throw bad("Could not read the CSV file"); }
    }

    private static Map<String,Integer> headers(List<String> c) { Map<String,Integer> m=new HashMap<>(); for(int i=0;i<c.size();i++){String h=normalize(c.get(i)); if(h.equals("month")||h.equals("monthname")||h.equals("date"))m.put("month",i); if(h.equals("year"))m.put("year",i); if(h.equals("totalusage")||h.equals("usage")||h.equals("totalusagekwh"))m.put("totalusage",i); if(h.equals("usageuom")||h.equals("unit"))m.put("usageuom",i);} return m; }
    private static int parseMonth(String value) { String v=value.trim(); try { return Integer.parseInt(v); } catch(Exception ignored) {} String n=normalize(v); for(Month m:Month.values()) if(normalize(m.name()).equals(n)||normalize(m.name().substring(0,3)).equals(n)) return m.getValue(); throw new IllegalArgumentException(); }
    private static int[] parseMonthYear(String value) { String[] parts=value.trim().split("[-/\\s]+", 2); if (parts.length != 2) throw new IllegalArgumentException(); int year; int month; if (parts[0].length() == 4) { year=Integer.parseInt(parts[0]); month=parseMonth(parts[1]); } else { month=parseMonth(parts[0]); year=Integer.parseInt(parts[1]); } return new int[]{month, year}; }
    private static List<String> cells(String line) { List<String> out=new ArrayList<>(); StringBuilder b=new StringBuilder(); boolean q=false; for(int i=0;i<line.length();i++){char x=line.charAt(i); if(x=='"')q=!q; else if(x==','&&!q){out.add(b.toString());b.setLength(0);} else b.append(x);} out.add(b.toString()); return out; }
    private static String normalize(String v){return v.replace("\uFEFF","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    private static BigDecimal money(BigDecimal v){return v.setScale(2,RoundingMode.HALF_UP);}
    private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private record ParsedRow(int month,int year,BigDecimal usage) {}
}
