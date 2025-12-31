package com.college.bookmyslot.service;

import com.college.bookmyslot.dto.BookSlotRequest;
import com.college.bookmyslot.model.SlotBooking;
import com.college.bookmyslot.model.TeacherSlot;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.EventBookingRepository;
import com.college.bookmyslot.repository.SlotBookingRepository;
import com.college.bookmyslot.repository.TeacherSlotRepository;
import com.college.bookmyslot.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SlotBookingService {

    private final TeacherSlotRepository slotRepository;
    private final SlotBookingRepository bookingRepository;
    private final EventBookingRepository eventBookingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public SlotBookingService(
            TeacherSlotRepository slotRepository,
            SlotBookingRepository bookingRepository,
            EventBookingRepository eventBookingRepository,
            UserRepository userRepository,
            EmailService emailService
    ) {
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
        this.eventBookingRepository = eventBookingRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public SlotBooking bookSlot(BookSlotRequest request) {

        TeacherSlot slot = slotRepository.findByIdForUpdate(request.getSlotId())
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (slot.getStatus() != TeacherSlot.Status.AVAILABLE) {
            throw new RuntimeException("Slot is not available");
        }

        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        boolean slotConflict =
                bookingRepository.existsOverlappingSlotBooking(
                        student,
                        slot.getDate(),
                        slot.getStartTime(),
                        slot.getEndTime()
                );

        boolean eventConflict =
                eventBookingRepository.existsOverlappingEventBooking(
                        student,
                        slot.getDate(),
                        slot.getStartTime(),
                        slot.getEndTime()
                );

        if (slotConflict || eventConflict) {
            throw new RuntimeException(
                    "You already have a booking during this time"
            );
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
        } catch (Exception ignored) {}

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
        } catch (Exception ignored) {}

        return saved;
    }
}
