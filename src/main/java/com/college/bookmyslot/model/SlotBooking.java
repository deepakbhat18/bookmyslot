package com.college.bookmyslot.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "slot_bookings")
public class SlotBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // which slot
    @ManyToOne
    @JoinColumn(name = "slot_id", nullable = false)
    private TeacherSlot slot;

    // which student
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    private LocalDateTime bookedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public enum Status {
        BOOKED,
        CANCELLED
    }

    // getters & setters

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public TeacherSlot getSlot() { return slot; }

    public void setSlot(TeacherSlot slot) { this.slot = slot; }

    public User getStudent() { return student; }

    public void setStudent(User student) { this.student = student; }

    public LocalDateTime getBookedAt() { return bookedAt; }

    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }

    public Status getStatus() { return status; }

    public void setStatus(Status status) { this.status = status; }
}
