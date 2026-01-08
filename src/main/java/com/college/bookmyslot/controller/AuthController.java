
package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.*;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.ClubRepository;
import com.college.bookmyslot.repository.UserRepository;
import com.college.bookmyslot.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")

public class AuthController {

    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final EmailService emailService;

    public AuthController(
            UserRepository userRepository,
            ClubRepository clubRepository,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.clubRepository = clubRepository;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        User.Role role;
        try {
            role = User.Role.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException("Invalid role. Use STUDENT or TEACHER");
        }

        if (role == User.Role.CLUB || role == User.Role.ADMIN) {
            throw new RuntimeException(
                    "This role cannot self-register. Please contact admin."
            );
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(role);

        if (role == User.Role.STUDENT) {
            if (request.getUsn() == null) {
                throw new RuntimeException("USN is required for students");
            }
            user.setUsn(request.getUsn());
        }

        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        user.setVerified(false);

        userRepository.save(user);

        try {
            emailService.sendOtpEmail(
                    user.getEmail(),
                    user.getName(),
                    otp,
                    "Account Verification"
            );
        } catch (Exception e) {
            System.err.println("OTP email failed: " + e.getMessage());
        }

        return new ApiResponse<>(
                true,
                "Registered successfully. Verify OTP sent to email.",
                null
        );
    }

@PostMapping("/verify-otp")
public ApiResponse<String> verifyOtp(@RequestBody OtpVerifyRequest request) {

    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

    if (user.isVerified()) {
        return new ApiResponse<>(true, "Already verified", null);
    }

    if (!request.getOtp().equals(user.getOtp()) ||
            user.getOtpExpiry().isBefore(LocalDateTime.now())) {
        throw new RuntimeException("Invalid or expired OTP");
    }

    user.setVerified(true);
    user.setOtp(null);
    user.setOtpExpiry(null);
    userRepository.save(user);

    return new ApiResponse<>(true, "Email verified successfully", null);
}


    @PostMapping("/resend-otp")
    public ApiResponse<String> resendOtp(@RequestBody ResendOtpRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isVerified()) {
            return new ApiResponse<>(true, "User already verified", null);
        }

        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendOtpEmail(
                user.getEmail(),
                user.getName(),
                otp,
                "Email Verification"
        );

        return new ApiResponse<>(
                true,
                "New OTP sent to your email",
                null
        );
    }

    @PostMapping("/login")
    public ApiResponse<String> login(
            @RequestBody LoginRequest request,
            HttpSession session
    ) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!user.isVerified()) {
            throw new RuntimeException("Please verify your email first");
        }

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }
        session.setAttribute("USER_ID", user.getId());
        session.setAttribute("ROLE", user.getRole().name());

        return new ApiResponse<>(
                true,
                "Login successful",
                null
        );
    }
    @PostMapping("/logout")
    public ApiResponse<String> logout(HttpSession session) {
        session.invalidate();
        return new ApiResponse<>(true, "Logged out successfully", null);
    }

    @PutMapping("/users/{id}/deactivate")
    public void deactivateUser(@PathVariable Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(false);
        userRepository.save(user);
    }
    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(
            @RequestBody ForgotPasswordRequest request
    ) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendOtpEmail(
                user.getEmail(),
                user.getName(),
                otp,
                "Password Reset"
        );

        return new ApiResponse<>(
                true,
                "Password reset OTP sent to email",
                null
        );
    }
    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(
            @RequestBody ResetPasswordRequest request
    ) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getResetOtp() == null ||
                !user.getResetOtp().equals(request.getOtp()) ||
                user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {

            throw new RuntimeException("Invalid or expired OTP");
        }

        user.setPassword(request.getNewPassword());
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);

        userRepository.save(user);

        return new ApiResponse<>(
                true,
                "Password reset successful",
                null
        );
    }

}
