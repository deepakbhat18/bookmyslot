package com.college.bookmyslot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Entity
@Table(name = "event_bookings")
public class EventBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(unique = true, nullable = false)
    private String ticketId;

    private boolean paid;
    private double amountPaid;
    private boolean reminderSent = false;


    @Column(nullable = false)
    private boolean checkedIn = false;

    private LocalDateTime checkedInAt;

    private LocalDateTime bookedAt = LocalDateTime.now();

}
