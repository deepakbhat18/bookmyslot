package com.college.bookmyslot.service;

import com.college.bookmyslot.dto.EventCheckInRequest;
import com.college.bookmyslot.dto.EventCheckInResponse;
import com.college.bookmyslot.model.Club;
import com.college.bookmyslot.model.EventBooking;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.EventBookingRepository;
import com.college.bookmyslot.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EventCheckInService {

    private final EventBookingRepository bookingRepository;
    private final UserRepository userRepository;

    public EventCheckInService(EventBookingRepository bookingRepository,
                               UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    public EventCheckInResponse checkIn(EventCheckInRequest request) {

        EventBooking booking = bookingRepository.findByTicketId(request.getTicketId())
                .orElseThrow(() -> new RuntimeException("Invalid ticket"));

        User staff = userRepository.findById(request.getStaffUserId())
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        if (staff.getRole() != User.Role.CLUB) {
            throw new RuntimeException("Only club staff can check-in");
        }

        Club eventClub = booking.getEvent().getClub();
        if (staff.getClub() == null ||
                !staff.getClub().getId().equals(eventClub.getId())) {
            throw new RuntimeException("Unauthorized staff");
        }

        if (booking.isCheckedIn()) {
            throw new RuntimeException("Ticket already checked-in");
        }

        booking.setCheckedIn(true);
        booking.setCheckedInAt(LocalDateTime.now());
        bookingRepository.save(booking);

        EventCheckInResponse response = new EventCheckInResponse();
        response.setTicketId(booking.getTicketId());
        response.setStudentName(booking.getStudent().getName());
        response.setEventTitle(booking.getEvent().getTitle());
        response.setCheckedIn(true);
        response.setCheckInTime(booking.getCheckedInAt());

        return response;
    }
}
