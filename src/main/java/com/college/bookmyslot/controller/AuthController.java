package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.*;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.UserRepository;
import com.college.bookmyslot.service.EmailService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final Random random = new Random();

    public AuthController(UserRepository userRepository,
                          EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @GetMapping("/test")
    public String test() {
        return "AuthController working ✅";
    }


    @PostMapping("/register")
    public User register(@RequestBody RegisterRequest request) {

        if (isBlank(request.getName()) ||
                isBlank(request.getEmail()) ||
                isBlank(request.getPassword()) ||
                isBlank(request.getRole())) {
            throw new RuntimeException("Name, email, password, role are required");
        }

        User.Role role;
        try {
            role = User.Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role. Use STUDENT, TEACHER or ADMIN");
        }

        if (role == User.Role.STUDENT && isBlank(request.getUsn())) {
            throw new RuntimeException("USN is required for students");
        }

        userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new RuntimeException("Email already registered");
        });

        if (role == User.Role.STUDENT && !isBlank(request.getUsn())) {
            userRepository.findByUsn(request.getUsn()).ifPresent(u -> {
                throw new RuntimeException("USN already registered");
            });
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setUsn(request.getUsn());
        user.setPassword(request.getPassword());
        user.setRole(role);


        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        user.setVerified(false);

        User saved = userRepository.save(user);


        try {
            emailService.sendWelcomeEmail(
                    saved.getEmail(),
                    saved.getName(),
                    saved.getRole().name(),
                    saved.getUsn()
            );
            emailService.sendOtpEmail(
                    saved.getEmail(),
                    saved.getName(),
                    otp,
                    "Email Verification"
            );
        } catch (Exception e) {
            System.err.println("Failed to send registration emails: " + e.getMessage());
        }

        return saved;
    }


    @PostMapping("/verify-otp")
    public Map<String, Object> verifyOtp(@RequestBody OtpVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOtp() == null || user.getOtpExpiry() == null) {
            throw new RuntimeException("No OTP generated");
        }
        if (!user.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }
        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        user.setVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Email verified successfully");
        resp.put("userId", user.getId());
        return resp;
    }


    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        if (!user.isVerified()) {
            throw new RuntimeException("Email not verified. Please verify using OTP.");
        }


        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Login successful");
        resp.put("userId", user.getId());
        resp.put("name", user.getName());
        resp.put("role", user.getRole().name());
        resp.put("email", user.getEmail());
        resp.put("usn", user.getUsn());
        return resp;
    }


    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = generateOtp();
        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        try {
            emailService.sendOtpEmail(
                    user.getEmail(),
                    user.getName(),
                    otp,
                    "Password Reset"
            );
        } catch (Exception e) {
            System.err.println("Failed to send reset password OTP: " + e.getMessage());
        }

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "OTP sent to email for password reset");
        return resp;
    }


    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@RequestBody ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getResetOtp() == null || user.getResetOtpExpiry() == null) {
            throw new RuntimeException("No reset OTP generated");
        }
        if (!user.getResetOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }
        if (user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        user.setPassword(request.getNewPassword());
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        userRepository.save(user);

        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Password reset successful");
        return resp;
    }



    private String generateOtp() {
        int num = 100000 + random.nextInt(900000); // 6-digit
        return String.valueOf(num);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
