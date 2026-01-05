package com.college.bookmyslot.repository;

import com.college.bookmyslot.model.Club;
import com.college.bookmyslot.model.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {


    List<Event> findByClub(Club club);
    List<Event> findByStatusOrderByEventDateAsc(Event.Status status);

    List<Event> findByEventDate(LocalDate eventDate);


    List<Event> findByStatus(Event.Status status);


    List<Event> findByEventDateAndStatus(
            LocalDate eventDate,
            Event.Status status
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :eventId")
    Optional<Event> findByIdForUpdate(@Param("eventId") Long eventId);

    Page<Event> findByStatusOrderByEventDateAsc(
            Event.Status status,
            Pageable pageable
    );
}
