package com.college.bookmyslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistrationsSummaryResponse {
    private Long eventId;
    private String eventTitle;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String venue;
    private String status;
    private long totalRegistrations;
    private long attendedCount;
    private long notAttendedCount;
    private List<EventRegistrationDto> registrations;
}
