package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.ApiResponse;
import com.college.bookmyslot.dto.EventCheckInRequest;
import com.college.bookmyslot.dto.EventCheckInResponse;
import com.college.bookmyslot.service.EventCheckInService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/checkin")
//@CrossOrigin(origins = "*")
public class EventCheckInController {

    private final EventCheckInService checkInService;

    public EventCheckInController(EventCheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping
    public ApiResponse<EventCheckInResponse> checkIn(
            @RequestBody EventCheckInRequest request
    ) {
        EventCheckInResponse response = checkInService.checkIn(request);

        return new ApiResponse<>(
                true,
                "Check-in successful",
                response
        );
    }
}
