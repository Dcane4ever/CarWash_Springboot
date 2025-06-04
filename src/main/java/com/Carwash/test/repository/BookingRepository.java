package com.Carwash.test.repository;

import com.Carwash.test.model.Booking;
import com.Carwash.test.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    // Add custom queries if needed
    List<Booking> findByUserId(Long userId);
    List<Booking> findByStatus(String status);
    List<Booking> findByUser(User user);
    
    // Add this new method for recent bookings
    List<Booking> findTop10ByOrderByBookingTimeDesc();
    
    // Add this new method for reports
    @Query("SELECT b FROM Booking b WHERE b.appointmentDate BETWEEN :startDate AND :endDate")
    List<Booking> findByAppointmentDateBetween(
        @Param("startDate") String startDate, 
        @Param("endDate") String endDate
    );

    // Add methods for slot availability
    @Query("SELECT b FROM Booking b WHERE b.appointmentDate = :date AND b.slotNumber = :slotNumber")
    List<Booking> findByDateAndSlot(@Param("date") String date, @Param("slotNumber") Integer slotNumber);

    @Query("SELECT b FROM Booking b WHERE b.appointmentDate = :date")
    List<Booking> findByDate(@Param("date") String date);
} 