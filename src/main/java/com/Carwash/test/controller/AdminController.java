package com.Carwash.test.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.Carwash.test.model.User;
import com.Carwash.test.model.Booking;
import com.Carwash.test.repository.UserRepository;
import com.Carwash.test.repository.BookingRepository;
import com.Carwash.test.util.SessionManager;
import java.util.List;
import java.util.Map;
import com.Carwash.test.service.BookingStatsService;
import com.Carwash.test.dto.BookingUpdateDTO;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.HashMap;
import com.Carwash.test.service.ReportService;
import java.time.temporal.WeekFields;
import com.Carwash.test.model.ServicePrice;
import com.Carwash.test.repository.ServicePriceRepository;
import java.time.LocalDateTime;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SessionManager sessionManager;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private BookingStatsService bookingStatsService;
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private ServicePriceRepository servicePriceRepository;
    
    // Admin credentials (in production, these should be in a secure configuration)
    private static final String ADMIN_USERNAME = "admin"; //Change this to whatever you want along with the password.
    private static final String ADMIN_PASSWORD = "admin123"; // In production, use hashed password
    
    @GetMapping("/login")
    public String adminLogin() {
        if (sessionManager.isAdminLoggedIn()) {
            return "redirect:/admin/dashboard";
        }
        return "admin-login";
    }

    @PostMapping("/login")
    public String adminLoginProcess(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            Model model) {
        
        if (username.equals(ADMIN_USERNAME) && password.equals(ADMIN_PASSWORD)) {
            sessionManager.setAdminSession();
            return "redirect:/admin/dashboard";
        } else {
            model.addAttribute("error", "Invalid admin credentials");
            return "admin-login";
        }
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        if (!sessionManager.isAdminLoggedIn()) {
            return "redirect:/admin/login";
        }
        
        // Get statistics
        Map<String, Object> stats = bookingStatsService.getBookingStats();
        model.addAttribute("stats", stats);
        
        // Get recent bookings
        List<Booking> recentBookings = bookingRepository.findTop10ByOrderByBookingTimeDesc();
        model.addAttribute("recentBookings", recentBookings);
        
        return "admin-dashboard";
    }

    @GetMapping("/customers")
    public String viewCustomers(
            @RequestParam(required = false) String search,
            Model model) {
        if (!sessionManager.isAdminLoggedIn()) {
            return "redirect:/admin/login";
        }
        
        try {
            List<User> customers;
            if (search != null && !search.trim().isEmpty()) {
                customers = userRepository.searchUsers(search.trim());
                model.addAttribute("searchTerm", search);
            } else {
                customers = userRepository.findAll();
            }
            
            model.addAttribute("customers", customers);
            return "admin-customers";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading customers: " + e.getMessage());
            return "admin-customers";
        }
    }

    @PostMapping("/customers/delete/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        if (!sessionManager.isAdminLoggedIn()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        
        try {
            if (!userRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                   .body("Customer not found");
            }
            
            // Get the user being deleted
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                               .body("Customer not found");
            }
            
            // Find all bookings for this user
            List<Booking> bookings = bookingRepository.findByUser(user);
            
            // Instead of deleting bookings, set user reference to null to preserve records
            for (Booking booking : bookings) {
                // Keep the booking but remove the user reference
                booking.setUser(null);
                // Optionally, store the username as a string to retain who made the booking
                booking.setNotes("Booking was made by " + user.getUsername() + " (account deleted)");
                bookingRepository.save(booking);
            }
            
            // Now delete the user
            userRepository.deleteById(id);
            return ResponseEntity.ok("Customer deleted successfully. Booking history has been preserved.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error deleting customer: " + e.getMessage());
        }
    }

    @GetMapping("/bookings")
    public String viewBookings(
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean paymentStatus,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String search,
            Model model) {
        
        if (!sessionManager.isAdminLoggedIn()) {
            return "redirect:/admin/login";
        }

        try {
            List<Booking> bookings = bookingRepository.findAll();

            // Apply filters
            if (dateRange != null && !dateRange.isEmpty()) {
                String[] dates = dateRange.split(" to ");
                LocalDate startDate = LocalDate.parse(dates[0]);
                LocalDate endDate = dates.length > 1 ? LocalDate.parse(dates[1]) : startDate;
                bookings = bookings.stream()
                    .filter(b -> {
                        LocalDate bookingDate = LocalDate.parse(b.getAppointmentDate());
                        return !bookingDate.isBefore(startDate) && !bookingDate.isAfter(endDate);
                    })
                    .collect(Collectors.toList());
            }

            if (status != null && !status.isEmpty()) {
                bookings = bookings.stream()
                    .filter(b -> b.getStatus().equals(status))
                    .collect(Collectors.toList());
            }

            if (paymentStatus != null) {
                bookings = bookings.stream()
                    .filter(b -> b.getPaid() == paymentStatus)
                    .collect(Collectors.toList());
            }

            if (serviceType != null && !serviceType.isEmpty()) {
                bookings = bookings.stream()
                    .filter(b -> b.getWashPackage().equals(serviceType))
                    .collect(Collectors.toList());
            }

            if (search != null && !search.isEmpty()) {
                String searchLower = search.toLowerCase();
                bookings = bookings.stream()
                    .filter(b -> 
                        b.getFullName().toLowerCase().contains(searchLower) ||
                        b.getEmail().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
            }

            model.addAttribute("bookings", bookings);
            model.addAttribute("activeFilters", buildActiveFilters(dateRange, status, paymentStatus, serviceType, search));
            
            return "admin-bookings";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading bookings: " + e.getMessage());
            return "admin-bookings";
        }
    }

    private Map<String, String> buildActiveFilters(String dateRange, String status, 
            Boolean paymentStatus, String serviceType, String search) {
        Map<String, String> filters = new HashMap<>();
        if (dateRange != null && !dateRange.isEmpty()) filters.put("Date Range", dateRange);
        if (status != null && !status.isEmpty()) filters.put("Status", status);
        if (paymentStatus != null) filters.put("Payment", paymentStatus ? "Paid" : "Unpaid");
        if (serviceType != null && !serviceType.isEmpty()) filters.put("Service", serviceType);
        if (search != null && !search.isEmpty()) filters.put("Search", search);
        return filters;
    }

    @PostMapping("/bookings/update/{id}")
    public ResponseEntity<?> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) Boolean isPaid) {
        if (!sessionManager.isAdminLoggedIn()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        
        try {
            Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            booking.setStatus(status);
            booking.setPaid(isPaid != null ? isPaid : false);
            bookingRepository.save(booking);
            
            return ResponseEntity.ok("Booking updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error updating booking: " + e.getMessage());
        }
    }

    @PostMapping("/bookings/bulk-update")
    public ResponseEntity<?> bulkUpdateBookings(@RequestBody List<BookingUpdateDTO> updates) {
        if (!sessionManager.isAdminLoggedIn()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        
        try {
            for (BookingUpdateDTO update : updates) {
                Booking booking = bookingRepository.findById(update.getId())
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + update.getId()));
                
                booking.setStatus(update.getStatus());
                booking.setPaid(update.getIsPaid());
                bookingRepository.save(booking);
            }
            
            return ResponseEntity.ok("All bookings updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error updating bookings: " + e.getMessage());
        }
    }

    @GetMapping("/logout")
    public String adminLogout() {
        sessionManager.logoutAdmin();
        return "redirect:/admin/login";
    }

    @GetMapping("/reports")
    public String viewReports(
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) List<String> washPackage,
            Model model) {
        
        if (!sessionManager.isAdminLoggedIn()) {
            return "redirect:/admin/login";
        }

        LocalDate start;
        LocalDate end;

        if (startDate != null && !startDate.isEmpty()) {
            start = LocalDate.parse(startDate);
        } else {
            start = LocalDate.now();
        }

        if (reportType == null || reportType.isEmpty()) {
            reportType = "daily";
        }

        switch (reportType.toLowerCase()) {
            case "weekly":
                start = start.with(WeekFields.ISO.dayOfWeek(), 1);
                end = start.plusDays(6);
                break;
            case "monthly":
                start = start.withDayOfMonth(1);
                end = start.plusMonths(1).minusDays(1);
                break;
            default: // daily
                end = start;
                break;
        }

        Map<String, Object> report = reportService.generateReport(reportType, start, end);

        // Filter bookings by washPackage if provided
        if (washPackage != null && !washPackage.isEmpty()) {
            Map<LocalDate, List<Booking>> bookingsByDate = (Map<LocalDate, List<Booking>>) report.get("bookingsByDate");
            if (bookingsByDate != null) {
                Map<LocalDate, List<Booking>> filteredBookingsByDate = new LinkedHashMap<>();
                for (Map.Entry<LocalDate, List<Booking>> entry : bookingsByDate.entrySet()) {
                    List<Booking> filteredList = entry.getValue().stream()
                        .filter(b -> washPackage.contains(b.getWashPackage()))
                        .collect(Collectors.toList());
                    if (!filteredList.isEmpty()) {
                        filteredBookingsByDate.put(entry.getKey(), filteredList);
                    }
                }
                report.put("bookingsByDate", filteredBookingsByDate);
            }
        }

        // Flatten bookings for the template
        List<Booking> flatBookings = new ArrayList<>();
        Map<LocalDate, List<Booking>> bookingsByDate = (Map<LocalDate, List<Booking>>) report.get("bookingsByDate");
        if (bookingsByDate != null) {
            for (Map.Entry<LocalDate, List<Booking>> entry : bookingsByDate.entrySet()) {
                for (Booking b : entry.getValue()) {
                    // Optionally set a displayDate property if Booking has it
                    // b.setDisplayDate(entry.getKey());
                    flatBookings.add(b);
                }
            }
        }
        model.addAttribute("flatBookings", flatBookings);

        model.addAttribute("report", report);
        model.addAttribute("reportType", reportType);
        model.addAttribute("startDate", start);
        Map<String, Object> param = new HashMap<>();
        param.put("washPackageList", washPackage != null ? washPackage : new ArrayList<>());
        model.addAttribute("param", param);
        
        // Add priceMap for use in the template
        List<ServicePrice> prices = servicePriceRepository.findAll();
        Map<String, Double> priceMap = new HashMap<>();
        for (ServicePrice sp : prices) {
            priceMap.put(sp.getServiceType(), sp.getPrice());
        }
        model.addAttribute("priceMap", priceMap);
        
        return "admin-reports";
    }

    @GetMapping("/reports/export/csv")
    public void exportReportAsCsv(
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String startDateParam,
            HttpServletResponse response) throws IOException {

        if (!sessionManager.isAdminLoggedIn()) {
            response.sendRedirect("/admin/login");
            return;
        }

        LocalDate start;
        LocalDate end;
        final String effectiveReportType = (reportType == null || reportType.isEmpty()) ? "daily" : reportType;
        final LocalDate effectiveStartDate = (startDateParam == null || startDateParam.isEmpty()) ? LocalDate.now() : LocalDate.parse(startDateParam);

        start = effectiveStartDate;

        switch (effectiveReportType.toLowerCase()) {
            case "weekly":
                start = start.with(WeekFields.ISO.dayOfWeek(), 1);
                end = start.plusDays(6);
                break;
            case "monthly":
                start = start.withDayOfMonth(1);
                end = start.plusMonths(1).minusDays(1);
                break;
            default: // daily
                end = start;
                break;
        }

        Map<String, Object> reportData = reportService.generateReport(effectiveReportType, start, end);

        response.setContentType("text/csv");
        String reportFileName = "carwash_report_" + effectiveReportType + "_" + start.toString();
        if (!effectiveReportType.equalsIgnoreCase("daily")) {
            reportFileName += "_to_" + end.toString();
        }
        reportFileName += ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + reportFileName + "\"");

        PrintWriter writer = response.getWriter();

        // CSV Header
        writer.println("Date,Plate Number,Full Name,Email,Phone Number,Vehicle Type,Wash Package,Price,Status,Payment Status");

        // CSV Data
        // Assuming reportData contains "bookingsByDate" which is a Map<LocalDate, List<Booking>>
        // And "dailyRevenue" which might be Map<LocalDate, Double> or Map<String, Double> if keys are formatted dates
        // We need to iterate through bookings. The structure of reportData.get("bookingsByDate") needs to be confirmed.
        // For now, let's assume it's Map<LocalDate, List<Booking>> and we sort by date.

        Object bookingsByDateObj = reportData.get("bookingsByDate");
        if (bookingsByDateObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, List<Booking>> bookingsByDateRaw = (Map<Object, List<Booking>>) bookingsByDateObj;

            // Convert keys to LocalDate and sort them
            Map<LocalDate, List<Booking>> bookingsByDate = bookingsByDateRaw.entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> {
                        if (entry.getKey() instanceof LocalDate) {
                            return (LocalDate) entry.getKey();
                        } else if (entry.getKey() instanceof String) {
                            // Attempt to parse common date formats if not ISO_DATE
                            try {
                                return LocalDate.parse((String) entry.getKey(), DateTimeFormatter.ISO_DATE);
                            } catch (java.time.format.DateTimeParseException e) {
                                // Add other parsers or a more robust solution if needed
                                return LocalDate.parse((String) entry.getKey(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                            }
                        }
                        // Add more sophisticated date parsing or throw an error if format is unknown
                        throw new IllegalArgumentException("Unknown date key type in bookingsByDate: " + entry.getKey().getClass());
                    },
                    Map.Entry::getValue,
                    (oldValue, newValue) -> oldValue, // merge function, not strictly needed if keys are unique
                    LinkedHashMap::new // preserve order after sorting
                ));
            
            List<LocalDate> sortedDates = new ArrayList<>(bookingsByDate.keySet());
            sortedDates.sort(Comparator.naturalOrder());

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (LocalDate dateKey : sortedDates) {
                List<Booking> bookingsOnDate = bookingsByDate.get(dateKey);
                if (bookingsOnDate != null) {
                    for (Booking booking : bookingsOnDate) {
                        ServicePrice servicePrice = servicePriceRepository.findByServiceType(booking.getWashPackage());
                        String priceString = "0.00";
                        if (servicePrice != null && servicePrice.getPrice() != null) {
                            priceString = String.format("%.2f", servicePrice.getPrice());
                        }

                        writer.println(String.join(",",
                            escapeCsv(dateKey.format(dateFormatter)),
                            escapeCsv(booking.getPlateNumber()),
                            escapeCsv(booking.getFullName()),
                            escapeCsv(booking.getEmail()),
                            escapeCsv(booking.getPhone()), // Corrected to use getPhone()
                            escapeCsv(booking.getVehicleType()),
                            escapeCsv(booking.getWashPackage()),
                            escapeCsv(priceString), // Use fetched and formatted price
                            escapeCsv(booking.getStatus()),
                            escapeCsv(booking.getPaid() ? "Paid" : "Unpaid")
                        ));
                    }
                }
            }
        }
        writer.flush();
        writer.close();
    }

    private String escapeCsv(String data) {
        if (data == null) {
            return "\"\"";
        }
        String escapedData = data.replaceAll("\\R", " "); // Replace newlines with space
        if (escapedData.contains(",") || escapedData.contains("\"") || escapedData.contains("'")) {
            escapedData = escapedData.replace("\"", "\"\"");
            escapedData = "\"" + escapedData + "\"";
        }
        return escapedData;
    }

    @GetMapping("/prices")
    public String viewPrices(Model model) {
        if (!sessionManager.isAdminLoggedIn()) {
            return "redirect:/admin/login";
        }
        
        // Check if prices exist, if not create default ones
        if (servicePriceRepository.count() == 0) {
            // Create basic wash price
            ServicePrice basic = new ServicePrice();
            basic.setServiceType("basic");
            basic.setPrice(1500.0);
            basic.setUpdatedAt(LocalDateTime.now());
            servicePriceRepository.save(basic);

            // Create premium wash price
            ServicePrice premium = new ServicePrice();
            premium.setServiceType("premium");
            premium.setPrice(2500.0);
            premium.setUpdatedAt(LocalDateTime.now());
            servicePriceRepository.save(premium);

            // Create express wash price
            ServicePrice express = new ServicePrice();
            express.setServiceType("express");
            express.setPrice(1000.0);
            express.setUpdatedAt(LocalDateTime.now());
            servicePriceRepository.save(express);
        }
        
        List<ServicePrice> prices = servicePriceRepository.findAll();
        model.addAttribute("prices", prices);
        return "admin-prices";
    }

    @PostMapping("/prices/update/{id}")
    public ResponseEntity<?> updatePrice(
            @PathVariable Long id,
            @RequestParam Double price) {
        if (!sessionManager.isAdminLoggedIn()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        
        try {
            ServicePrice servicePrice = servicePriceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Price not found"));
            
            servicePrice.setPrice(price);
            servicePrice.setUpdatedAt(LocalDateTime.now());
            servicePriceRepository.save(servicePrice);
            
            return ResponseEntity.ok("Price updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error updating price: " + e.getMessage());
        }
    }
}
