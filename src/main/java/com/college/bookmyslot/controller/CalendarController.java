package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.CalendarEventDto;
import com.college.bookmyslot.model.SlotBooking;
import com.college.bookmyslot.model.TeacherSlot;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.SlotBookingRepository;
import com.college.bookmyslot.repository.TeacherSlotRepository;
import com.college.bookmyslot.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@CrossOrigin(origins = "*")
public class CalendarController {

    private final TeacherSlotRepository slotRepository;
    private final SlotBookingRepository bookingRepository;
    private final UserRepository userRepository;

    public CalendarController(TeacherSlotRepository slotRepository,
                              SlotBookingRepository bookingRepository,
                              UserRepository userRepository) {
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    // ===== TEACHER VIEW: all their slots and bookings as calendar events =====
    @GetMapping("/teacher/{teacherId}")
    public List<CalendarEventDto> getTeacherCalendar(@PathVariable Long teacherId) {

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        List<CalendarEventDto> events = new ArrayList<>();

        // 1) Teacher's own slots
        List<TeacherSlot> slots = slotRepository.findByTeacher(teacher);
        for (TeacherSlot slot : slots) {
            CalendarEventDto dto = new CalendarEventDto();
            dto.setTitle("Slot (" + slot.getStatus().name() + ")");
            LocalDateTime start = LocalDateTime.of(slot.getDate(), slot.getStartTime());
            LocalDateTime end = LocalDateTime.of(slot.getDate(), slot.getEndTime());
            dto.setStart(start.toString());
            dto.setEnd(end.toString());
            dto.setTeacherName(teacher.getName());
            dto.setStudentName(null);
            dto.setType("TEACHER_SLOT");
            events.add(dto);
        }

        // 2) Bookings on those slots
        List<SlotBooking> bookings = bookingRepository.findBySlot_Teacher(teacher);
        for (SlotBooking booking : bookings) {
            CalendarEventDto dto = new CalendarEventDto();
            dto.setTitle("Booked by " + booking.getStudent().getName());
            TeacherSlot slot = booking.getSlot();
            LocalDateTime start = LocalDateTime.of(slot.getDate(), slot.getStartTime());
            LocalDateTime end = LocalDateTime.of(slot.getDate(), slot.getEndTime());
            dto.setStart(start.toString());
            dto.setEnd(end.toString());
            dto.setTeacherName(teacher.getName());
            dto.setStudentName(booking.getStudent().getName());
            dto.setType("STUDENT_BOOKING");
            events.add(dto);
        }

        return events;
    }

    // ===== STUDENT VIEW: all their bookings as calendar events =====
    @GetMapping("/student/{studentId}")
    public List<CalendarEventDto> getStudentCalendar(@PathVariable Long studentId) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<SlotBooking> bookings = bookingRepository.findByStudent(student);

        List<CalendarEventDto> events = new ArrayList<>();

        for (SlotBooking booking : bookings) {
            TeacherSlot slot = booking.getSlot();

            CalendarEventDto dto = new CalendarEventDto();
            dto.setTitle("Slot with " + slot.getTeacher().getName());
            LocalDateTime start = LocalDateTime.of(slot.getDate(), slot.getStartTime());
            LocalDateTime end = LocalDateTime.of(slot.getDate(), slot.getEndTime());
            dto.setStart(start.toString());
            dto.setEnd(end.toString());
            dto.setTeacherName(slot.getTeacher().getName());
            dto.setStudentName(student.getName());
            dto.setType("STUDENT_BOOKING");

            events.add(dto);
        }

        return events;
    }
}
