package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.AdminStaffResponse;
import com.college.bookmyslot.dto.ApiResponse;
import com.college.bookmyslot.dto.ClubStaffCreateRequest;
import com.college.bookmyslot.model.Club;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.ClubRepository;
import com.college.bookmyslot.repository.UserRepository;
import com.college.bookmyslot.service.EmailService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

//
//@RestController
//@RequestMapping("/api/admin/staff")
//public class AdminStaffController {
//
//    private final UserRepository userRepository;
//    private final EmailService emailService;
//
//    public AdminStaffController(
//            UserRepository userRepository,
//            EmailService emailService
//    ) {
//        this.userRepository = userRepository;
//        this.emailService = emailService;
//    }
//
//
//    @GetMapping
//    public List<User> getAllClubStaff() {
//        return userRepository.findAll()
//                .stream()
//                .filter(u -> u.getRole() == User.Role.CLUB)
//                .toList();
//    }
//
//
//    @GetMapping("/{staffId}")
//    public User getStaffById(@PathVariable Long staffId) {
//        return userRepository.findById(staffId)
//                .orElseThrow(() -> new RuntimeException("Staff not found"));
//    }
//
//
//    @PutMapping("/{staffId}/deactivate")
//    public ApiResponse<String> deactivateStaff(@PathVariable Long staffId) {
//
//        User staff = userRepository.findById(staffId)
//                .orElseThrow(() -> new RuntimeException("Staff not found"));
//
//        if (staff.getRole() != User.Role.CLUB) {
//            throw new RuntimeException("Not a club staff");
//        }
//
//        staff.setActive(false);
//        userRepository.save(staff);
//
//        return new ApiResponse<>(
//                true,
//                "Staff deactivated successfully",
//                null
//        );
//    }
//    @PostMapping("/{staffId}/reset-password")
//    public ApiResponse<String> resetStaffPassword(@PathVariable Long staffId) {
//
//        User staff = userRepository.findById(staffId)
//                .orElseThrow(() -> new RuntimeException("Staff not found"));
//
//        if (staff.getRole() != User.Role.CLUB) {
//            throw new RuntimeException("Not a club staff");
//        }
//
//        String newPassword = "Club@" + staffId; // simple temp password
//        staff.setPassword(newPassword);
//
//        userRepository.save(staff);
//
//
//        emailService.sendWelcomeEmail(
//                staff.getEmail(),
//                staff.getName(),
//                "CLUB STAFF (Password Reset)",
//                null
//        );
//
//        return new ApiResponse<>(
//                true,
//                "Password reset & email sent",
//                null
//        );
//    }
//}
@RestController
@RequestMapping("/api/admin/staff")
public class AdminStaffController {

    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final EmailService emailService;

    public AdminStaffController(
            UserRepository userRepository,
            ClubRepository clubRepository,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.clubRepository = clubRepository;
        this.emailService = emailService;
    }

    // 1️⃣ LIST ALL CLUB STAFF
    @GetMapping
    public List<AdminStaffResponse> getAllStaff() {
        return userRepository.findByRole(User.Role.CLUB)
                .stream()
                .map(staff -> new AdminStaffResponse(
                        staff.getId(),
                        staff.getName(),
                        staff.getEmail(),
                        staff.getClub().getName(),
                        staff.isActive()
                ))
                .toList();
    }

    // 2️⃣ STAFF DETAILS
    @GetMapping("/{staffId}")
    public AdminStaffResponse getStaff(@PathVariable Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        return new AdminStaffResponse(
                staff.getId(),
                staff.getName(),
                staff.getEmail(),
                staff.getClub().getName(),
                staff.isActive()
        );
    }

    // 3️⃣ CREATE / REPLACE CLUB STAFF
    @PostMapping
    public ApiResponse<String> createStaff(@RequestBody ClubStaffCreateRequest request) {

        Club club = clubRepository.findById(request.getClubId())
                .orElseThrow(() -> new RuntimeException("Club not found"));

        // 🔒 BLOCK if active staff already exists
        boolean hasActiveStaff =
                userRepository.existsByClubAndActive(club, true);

        if (hasActiveStaff) {
            throw new RuntimeException(
                    "This club already has an active staff. Deactivate first."
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User staff = new User();
        staff.setName(request.getName());
        staff.setEmail(request.getEmail());
        staff.setPassword(request.getPassword()); // temp password
        staff.setRole(User.Role.CLUB);
        staff.setClub(club);
        staff.setVerified(true);
        staff.setActive(true);

        userRepository.save(staff);

        // 📧 SEND EMAIL CREDENTIALS
        emailService.sendClubStaffCreatedEmail(
                staff.getEmail(),
                staff.getName(),
                club.getName(),
                request.getPassword()
        );

        return new ApiResponse<>(
                true,
                "Club staff created and email sent",
                null
        );
    }

    // 4️⃣ DEACTIVATE STAFF
    @PutMapping("/{staffId}/deactivate")
    public ApiResponse<String> deactivateStaff(@PathVariable Long staffId) {

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        staff.setActive(false);
        userRepository.save(staff);

        return new ApiResponse<>(
                true,
                "Staff deactivated",
                null
        );
    }


    @PostMapping("/{staffId}/reset-password")
    public ApiResponse<String> resetPassword(@PathVariable Long staffId) {

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        staff.setResetOtp(otp);
        staff.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(staff);

        emailService.sendOtpEmail(
                staff.getEmail(),
                staff.getName(),
                otp,
                "Password Reset"
        );

        return new ApiResponse<>(
                true,
                "Password reset OTP sent to email",
                null
        );
    }
}
