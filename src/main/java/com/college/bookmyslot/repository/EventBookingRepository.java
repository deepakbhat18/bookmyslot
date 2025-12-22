package com.college.bookmyslot.repository;

import com.college.bookmyslot.model.Event;
import com.college.bookmyslot.model.EventBooking;
import com.college.bookmyslot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventBookingRepository extends JpaRepository<EventBooking, Long> {

    List<EventBooking> findByEvent(Event event);

    List<EventBooking> findByStudent(User student);

    Optional<EventBooking> findByEventAndStudent(Event event, User student);

    Optional<EventBooking> findByTicketId(String ticketId);

    long countByEvent(Event event);
}
