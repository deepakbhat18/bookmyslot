package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.DailyCountDto;
import com.college.bookmyslot.dto.TeacherUsageDto;
import com.college.bookmyslot.model.SlotBooking;
import com.college.bookmyslot.model.TeacherSlot;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.SlotBookingRepository;
import com.college.bookmyslot.repository.TeacherSlotRepository;
import com.college.bookmyslot.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final UserRepository userRepository;
    private final TeacherSlotRepository slotRepository;
    private final SlotBookingRepository bookingRepository;

    public AnalyticsController(UserRepository userRepository,
                               TeacherSlotRepository slotRepository,
                               SlotBookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
    }

    // ===== OVERVIEW (same as before) =====
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, Object> resp = new HashMap<>();

        List<User> users = userRepository.findAll();

        long totalUsers = users.size();
        long totalStudents = users.stream().filter(u -> u.getRole() == User.Role.STUDENT).count();
        long totalTeachers = users.stream().filter(u -> u.getRole() == User.Role.TEACHER).count();
        long totalAdmins = users.stream().filter(u -> u.getRole() == User.Role.ADMIN).count();

        long totalSlots = slotRepository.count();
        long totalBookings = bookingRepository.count();

        resp.put("totalUsers", totalUsers);
        resp.put("totalStudents", totalStudents);
        resp.put("totalTeachers", totalTeachers);
        resp.put("totalAdmins", totalAdmins);
        resp.put("totalSlots", totalSlots);
        resp.put("totalBookings", totalBookings);

        return resp;
    }


    @GetMapping("/daily-bookings")
    public List<DailyCountDto> dailyBookings(@RequestParam(defaultValue = "7") int days) {

        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(days - 1); // include today

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay().minusSeconds(1);

        List<SlotBooking> bookings = bookingRepository.findByBookedAtBetween(from, to);


        Map<LocalDate, Long> counts = bookings.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getBookedAt().toLocalDate(),
                        Collectors.counting()
                ));

        List<DailyCountDto> result = new ArrayList<>();


        for (int i = 0; i < days; i++) {
            LocalDate d = fromDate.plusDays(i);
            long count = counts.getOrDefault(d, 0L);
            result.add(new DailyCountDto(d.toString(), count));
        }

        return result;
    }

    @GetMapping("/teacher-usage")
    public List<TeacherUsageDto> teacherUsage() {

        // all teachers
        List<User> teachers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.TEACHER)
                .collect(Collectors.toList());


        List<TeacherSlot> allSlots = slotRepository.findAll();
        List<SlotBooking> allBookings = bookingRepository.findAll();

        List<TeacherUsageDto> result = new ArrayList<>();

        for (User teacher : teachers) {
            TeacherUsageDto dto = new TeacherUsageDto();
            dto.setTeacherId(teacher.getId());
            dto.setTeacherName(teacher.getName());

            long totalSlots = allSlots.stream()
                    .filter(s -> s.getTeacher().getId().equals(teacher.getId()))
                    .count();

            long totalBookings = allBookings.stream()
                    .filter(b -> b.getSlot().getTeacher().getId().equals(teacher.getId()))
                    .count();

            dto.setTotalSlots(totalSlots);
            dto.setTotalBookings(totalBookings);

            result.add(dto);
        }

        return result;
    }
}
