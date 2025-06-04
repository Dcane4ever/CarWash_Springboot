package com.Carwash.test.service;

import com.Carwash.test.model.Booking;
import com.Carwash.test.repository.BookingRepository;
import com.Carwash.test.repository.ServicePriceRepository;
import com.Carwash.test.model.ServicePrice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingStatsService {
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private ServicePriceRepository servicePriceRepository;
    
    public Map<String, Object> getBookingStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Booking> allBookings = bookingRepository.findAll();
        
        // Today's bookings
        LocalDate today = LocalDate.now();
        long todayBookings = allBookings.stream()
            .filter(b -> b.getAppointmentDate() != null && 
                   b.getAppointmentDate().equals(today.toString()))
            .count();
        stats.put("todayBookings", todayBookings);
        
        // Total bookings
        stats.put("totalBookings", allBookings.size());
        
        // Payment statistics
        long paidBookings = allBookings.stream()
            .filter(b -> b.getPaid() != null && b.getPaid())
            .count();
        stats.put("paidBookings", paidBookings);
        stats.put("unpaidBookings", allBookings.size() - paidBookings);
        
        // Status distribution
        Map<String, Long> statusCounts = allBookings.stream()
            .collect(Collectors.groupingBy(
                Booking::getStatus,
                Collectors.counting()
            ));
        stats.put("statusCounts", statusCounts);
        
        // Service type distribution
        Map<String, Long> serviceTypes = allBookings.stream()
            .collect(Collectors.groupingBy(
                Booking::getWashPackage,
                Collectors.counting()
            ));
        stats.put("serviceTypes", serviceTypes);
        
        return stats;
    }

    private double calculateRevenue(List<Booking> bookings) {
        return bookings.stream()
            .filter(b -> b.getPaid() != null && b.getPaid())
            .mapToDouble(this::getServicePrice)
            .sum();
    }

    private double getServicePrice(Booking booking) {
        ServicePrice servicePrice = servicePriceRepository.findByServiceType(booking.getWashPackage().toLowerCase());
        return servicePrice != null ? servicePrice.getPrice() : 0.0;
    }
} 