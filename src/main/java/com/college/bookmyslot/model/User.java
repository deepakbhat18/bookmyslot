package com.college.bookmyslot.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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
    private String usn; // only for students

    @Column(nullable = false)
    private String password; // plain text for now (NOT secure in real life)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

  
    private String otp;
    private LocalDateTime otpExpiry;
    private boolean verified = false;

    private String resetOtp;
    private LocalDateTime resetOtpExpiry;

    public enum Role {
        STUDENT,
        TEACHER,
        ADMIN
    }
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getUsn() { return usn; }

    public void setUsn(String usn) { this.usn = usn; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }

    public void setRole(Role role) { this.role = role; }

    public String getOtp() { return otp; }

    public void setOtp(String otp) { this.otp = otp; }

    public LocalDateTime getOtpExpiry() { return otpExpiry; }

    public void setOtpExpiry(LocalDateTime otpExpiry) { this.otpExpiry = otpExpiry; }

    public boolean isVerified() { return verified; }

    public void setVerified(boolean verified) { this.verified = verified; }

    public String getResetOtp() { return resetOtp; }

    public void setResetOtp(String resetOtp) { this.resetOtp = resetOtp; }

    public LocalDateTime getResetOtpExpiry() { return resetOtpExpiry; }

    public void setResetOtpExpiry(LocalDateTime resetOtpExpiry) { this.resetOtpExpiry = resetOtpExpiry; }
}
