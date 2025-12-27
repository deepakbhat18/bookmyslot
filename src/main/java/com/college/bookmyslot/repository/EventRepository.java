package com.college.bookmyslot.repository;

import com.college.bookmyslot.model.Club;
import com.college.bookmyslot.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {


    List<Event> findByClub(Club club);
    List<Event> findByStatusOrderByEventDateAsc(Event.Status status);

    List<Event> findByEventDate(LocalDate eventDate);


    List<Event> findByStatus(Event.Status status);


    List<Event> findByEventDateAndStatus(
            LocalDate eventDate,
            Event.Status status
    );
}
