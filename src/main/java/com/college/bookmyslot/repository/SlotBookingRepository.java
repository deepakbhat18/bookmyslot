package com.college.bookmyslot.repository;

import com.college.bookmyslot.model.SlotBooking;
import com.college.bookmyslot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SlotBookingRepository extends JpaRepository<SlotBooking, Long> {

    List<SlotBooking> findByStudent(User student);

    List<SlotBooking> findBySlot_Teacher(User teacher);

    List<SlotBooking> findByBookedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByBookedAtBetween(LocalDateTime start, LocalDateTime end);
}
