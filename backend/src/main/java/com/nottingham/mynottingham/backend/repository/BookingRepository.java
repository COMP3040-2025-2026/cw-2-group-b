package com.nottingham.mynottingham.backend.repository;

import com.nottingham.mynottingham.backend.entity.Booking;
import com.nottingham.mynottingham.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser(User user);
    List<Booking> findByStatus(Booking.BookingStatus status);
    List<Booking> findByFacilityName(String facilityName);
    List<Booking> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}
