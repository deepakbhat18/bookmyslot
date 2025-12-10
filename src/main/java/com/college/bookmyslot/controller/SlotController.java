package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.BookSlotRequest;
import com.college.bookmyslot.dto.CreateSlotRequest;
import com.college.bookmyslot.model.*;
import com.college.bookmyslot.repository.*;
import com.college.bookmyslot.service.EmailService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
@CrossOrigin(origins = "*")
public class SlotController {

    private final TeacherSlotRepository slotRepository;
    private final SlotBookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public SlotController(TeacherSlotRepository slotRepository,
                          SlotBookingRepository bookingRepository,
                          UserRepository userRepository,
                          EmailService emailService) {
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

  //This is used by the teacher to create slot
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

    //LIST AVAILABLE SLOTS BY DATE
    @GetMapping("/available")
    public List<TeacherSlot> getAvailableSlots(@RequestParam String date) {
        LocalDate d = LocalDate.parse(date);
        return slotRepository.findByDateAndStatus(d, TeacherSlot.Status.AVAILABLE);
    }


    // STUDENT BOOKS SLOT
    @PostMapping("/book")
    public SlotBooking bookSlot(@RequestBody BookSlotRequest request) {

        TeacherSlot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (slot.getStatus() != TeacherSlot.Status.AVAILABLE) {
            throw new RuntimeException("Slot is not available");
        }

        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getRole() != User.Role.STUDENT) {
            throw new RuntimeException("User is not a student");
        }

        SlotBooking booking = new SlotBooking();
        booking.setSlot(slot);
        booking.setStudent(student);
        booking.setBookedAt(LocalDateTime.now());
        booking.setStatus(SlotBooking.Status.BOOKED);


        slot.setStatus(TeacherSlot.Status.BOOKED);
        slotRepository.save(slot);

        SlotBooking saved = bookingRepository.save(booking);


        try {
            emailService.sendSlotBookingEmail(
                    student.getEmail(),
                    student.getName(),
                    slot.getTeacher().getName(),
                    slot.getDate().toString(),
                    slot.getStartTime().toString(),
                    slot.getEndTime().toString()
            );
        } catch (Exception e) {
            System.err.println("Failed to send slot booking email to student: " + e.getMessage());
        }


        try {
            emailService.sendTeacherSlotBookedEmail(
                    slot.getTeacher().getEmail(),
                    slot.getTeacher().getName(),
                    student.getName(),
                    student.getEmail(),
                    student.getUsn(),
                    slot.getDate().toString(),
                    slot.getStartTime().toString(),
                    slot.getEndTime().toString()
            );
        } catch (Exception e) {
            System.err.println("Failed to send slot booking email to teacher: " + e.getMessage());
        }

        return saved;
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
