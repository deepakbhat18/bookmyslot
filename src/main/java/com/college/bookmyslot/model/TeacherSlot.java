package com.college.bookmyslot.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "teacher_slots")
public class TeacherSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // teacher is a User with role TEACHER
    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    private LocalDate date;

    private LocalTime startTime;

    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public enum Status {
        AVAILABLE,
        BOOKED,
        BLOCKED
    }

    // getters & setters

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public User getTeacher() { return teacher; }

    public void setTeacher(User teacher) { this.teacher = teacher; }

    public LocalDate getDate() { return date; }

    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }

    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }

    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Status getStatus() { return status; }

    public void setStatus(Status status) { this.status = status; }
}
