package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.BookSlotRequest;
import com.college.bookmyslot.dto.CreateSlotRequest;
import com.college.bookmyslot.model.SlotBooking;
import com.college.bookmyslot.model.TeacherSlot;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.SlotBookingRepository;
import com.college.bookmyslot.repository.TeacherSlotRepository;
import com.college.bookmyslot.repository.UserRepository;
import com.college.bookmyslot.service.SlotBookingService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
//@CrossOrigin(origins = "*")
public class SlotController {

    private final TeacherSlotRepository slotRepository;
    private final SlotBookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SlotBookingService slotBookingService;

    public SlotController(
            TeacherSlotRepository slotRepository,
            SlotBookingRepository bookingRepository,
            UserRepository userRepository,
            SlotBookingService slotBookingService
    ) {
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.slotBookingService = slotBookingService;
    }

    @PostMapping("/create")
    public TeacherSlot createSlot(@RequestBody CreateSlotRequest request) {

        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        if (teacher.getRole() != User.Role.TEACHER) {
            throw new RuntimeException("User is not a teacher");
        }

        TeacherSlot slot = new TeacherSlot();
        slot.setTeacher(teacher);
        slot.setDate(LocalDate.parse(request.getDate()));
        slot.setStartTime(LocalTime.parse(request.getStartTime()));
        slot.setEndTime(LocalTime.parse(request.getEndTime()));
        slot.setStatus(TeacherSlot.Status.AVAILABLE);

        return slotRepository.save(slot);
    }

    @GetMapping("/available")
    public List<TeacherSlot> getAvailableSlots(@RequestParam String date) {
        LocalDate d = LocalDate.parse(date);
        return slotRepository.findByDateAndStatus(d, TeacherSlot.Status.AVAILABLE);
    }

    @PostMapping("/book")
    public SlotBooking bookSlot(@RequestBody BookSlotRequest request) {
        return slotBookingService.bookSlot(request);
    }

    @GetMapping("/student/{studentId}")
    public List<SlotBooking> getStudentBookings(@PathVariable Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return bookingRepository.findByStudent(student);
    }

    @GetMapping("/teacher/{teacherId}")
    public List<SlotBooking> getTeacherBookings(@PathVariable Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        return bookingRepository.findBySlot_Teacher(teacher);
    }
}
