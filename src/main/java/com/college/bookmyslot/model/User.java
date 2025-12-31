package com.college.bookmyslot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String usn;
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "club_id")
    private Club club;

    private String otp;
    private LocalDateTime otpExpiry;
    private boolean verified = false;

    private String resetOtp;
    private LocalDateTime resetOtpExpiry;

    public enum Role {
        STUDENT,
        TEACHER,
        CLUB,
        ADMIN
    }
    @Column(nullable = false)
    private boolean active = true;

}
