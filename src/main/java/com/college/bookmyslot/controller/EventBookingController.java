package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.ApiResponse;
import com.college.bookmyslot.dto.MyEventBookingResponse;
import com.college.bookmyslot.dto.EventBookingRequest;
import com.college.bookmyslot.dto.EventBookingResponse;
import com.college.bookmyslot.service.EventBookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/bookings")
//@CrossOrigin(origins = "*")
public class EventBookingController {

    private final EventBookingService eventBookingService;

    public EventBookingController(EventBookingService eventBookingService) {
        this.eventBookingService = eventBookingService;
    }

    @PostMapping("/book")
    public ApiResponse<EventBookingResponse> bookEvent(
            @RequestBody EventBookingRequest request
    ) {

        EventBookingResponse response = eventBookingService.bookEvent(request);

        return new ApiResponse<>(
                true,
                "Event booked successfully",
                response
        );
    }
    @PostMapping("/cancel/{bookingId}")
    public ApiResponse<String> cancelBooking(
            @PathVariable Long bookingId,
            HttpSession session
    ) {
        Long userId = (Long) session.getAttribute("USER_ID");

        if (userId == null) {
            throw new RuntimeException("Login required");
        }

        eventBookingService.cancelBooking(bookingId, userId);

        return new ApiResponse<>(
                true,
                "Booking cancelled successfully",
                null
        );
    }
    @GetMapping("/my")
    public ApiResponse<List<MyEventBookingResponse>> myBookings(
            HttpSession session
    ) {
        Long userId = (Long) session.getAttribute("USER_ID");

        if (userId == null) {
            throw new RuntimeException("Login required");
        }

        return new ApiResponse<>(
                true,
                "My bookings",
                eventBookingService.getMyBookings(userId)
        );
    }

}
