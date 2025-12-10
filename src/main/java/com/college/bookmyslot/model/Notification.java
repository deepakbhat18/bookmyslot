package com.college.bookmyslot.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String title;

    @Column(length = 1000)
    private String message;

    private boolean readFlag = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

    public boolean isReadFlag() { return readFlag; }

    public void setReadFlag(boolean readFlag) { this.readFlag = readFlag; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
