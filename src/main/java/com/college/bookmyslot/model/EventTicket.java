package com.college.bookmyslot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "event_tickets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "student_id"})
)
public class EventTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, unique = true)
    private String ticketCode = UUID.randomUUID().toString();

    private String qrCodeData;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.NA;

    @Enumerated(EnumType.STRING)
    private CheckInStatus checkInStatus = CheckInStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum PaymentStatus {
        NA,
        SUCCESS,
        FAILED
    }

    public enum CheckInStatus {
        PENDING,
        CHECKED_IN
    }

    // getters & setters
    public Long getId() { return id; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public String getTicketCode() { return ticketCode; }

    public String getQrCodeData() { return qrCodeData; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public CheckInStatus getCheckInStatus() { return checkInStatus; }
    public void setCheckInStatus(CheckInStatus checkInStatus) {
        this.checkInStatus = checkInStatus;
    }
}
