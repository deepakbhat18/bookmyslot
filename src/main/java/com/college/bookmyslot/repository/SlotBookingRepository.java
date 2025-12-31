package com.college.bookmyslot.repository;

import com.college.bookmyslot.model.EventBooking;
import com.college.bookmyslot.model.SlotBooking;
import com.college.bookmyslot.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface SlotBookingRepository extends JpaRepository<SlotBooking, Long> {

    List<SlotBooking> findByStudent(User student);

    List<SlotBooking> findBySlot_Teacher(User teacher);

    List<SlotBooking> findByBookedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByBookedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
SELECT COUNT(sb) > 0
FROM SlotBooking sb
WHERE sb.student = :student
  AND sb.slot.date = :date
  AND sb.slot.startTime < :endTime
  AND sb.slot.endTime > :startTime
  AND sb.status = 'BOOKED'
""")
    boolean existsOverlappingSlotBooking(
            @Param("student") User student,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

}
