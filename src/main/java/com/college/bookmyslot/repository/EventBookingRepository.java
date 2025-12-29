package com.college.bookmyslot.repository;

import com.college.bookmyslot.model.Event;
import com.college.bookmyslot.model.EventBooking;
import com.college.bookmyslot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface EventBookingRepository extends JpaRepository<EventBooking, Long> {

    List<EventBooking> findByEvent(Event event);

    List<EventBooking> findByStudent(User student);

    Optional<EventBooking> findByEventAndStudent(Event event, User student);

    Optional<EventBooking> findByTicketId(String ticketId);

    long countByEvent(Event event);
    @Query("""
SELECT eb FROM EventBooking eb
JOIN eb.event e
WHERE e.eventDate = :date
AND e.startTime BETWEEN :from AND :to
AND eb.reminderSent = false
""")
    List<EventBooking> findUpcomingBookings(
            @Param("date") LocalDate date,
            @Param("from") LocalTime from,
            @Param("to") LocalTime to
    );

}
