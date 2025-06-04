package com.Carwash.test.service;

import com.Carwash.test.model.Booking;
import com.Carwash.test.model.ServicePrice;
import com.Carwash.test.repository.BookingRepository;
import com.Carwash.test.repository.ServicePriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private ServicePriceRepository servicePriceRepository;

    public Map<String, Object> generateReport(String reportType, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();
        List<Booking> bookings = bookingRepository.findByAppointmentDateBetween(
            startDate.toString(), 
            endDate.toString()
        );
        
        // Calculate total revenue and bookings
        double totalRevenue = calculateRevenue(bookings);
        report.put("totalRevenue", totalRevenue);
        report.put("totalBookings", bookings.size());
        report.put("unpaidBookings", bookings.stream().filter(b -> !b.getPaid()).count());
        
        // Group bookings by date
        Map<LocalDate, List<Booking>> bookingsByDate = bookings.stream()
            .collect(Collectors.groupingBy(
                b -> LocalDate.parse(b.getAppointmentDate()),
                Collectors.toList()
            ));
        
        // Calculate daily revenue
        Map<LocalDate, Double> dailyRevenue = bookingsByDate.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> calculateRevenue(e.getValue())
            ));
        
        report.put("dailyRevenue", dailyRevenue);
        report.put("bookingsByDate", bookingsByDate);
        
        // Service type distribution
        Map<String, Long> serviceTypes = bookings.stream()
            .collect(Collectors.groupingBy(
                Booking::getWashPackage,
                Collectors.counting()
            ));
        report.put("serviceDistribution", serviceTypes);
        
        // Status distribution
        Map<String, Long> statusCounts = bookings.stream()
            .collect(Collectors.groupingBy(
                Booking::getStatus,
                Collectors.counting()
            ));
        report.put("statusDistribution", statusCounts);
        
        report.put("startDate", startDate);
        return report;
    }

    private double calculateRevenue(List<Booking> bookings) {
        return bookings.stream()
            .filter(b -> b.getPaid())
            .mapToDouble(this::getServicePrice)
            .sum();
    }

    private double getServicePrice(Booking booking) {
        ServicePrice servicePrice = servicePriceRepository.findByServiceType(booking.getWashPackage().toLowerCase());
        return servicePrice != null ? servicePrice.getPrice() : 0.0;
    }
} 