package com.college.bookmyslot.repository;

import com.college.bookmyslot.model.Event;
import com.college.bookmyslot.model.EventTicket;
import com.college.bookmyslot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventTicketRepository extends JpaRepository<EventTicket, Long> {


    boolean existsByEventAndStudent(Event event, User student);


    List<EventTicket> findByStudent(User student);


    List<EventTicket> findByEvent(Event event);


    Optional<EventTicket> findByTicketCode(String ticketCode);


    List<EventTicket> findByPaymentStatus(EventTicket.PaymentStatus status);
}

