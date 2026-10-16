package com.college.bookmyslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistrationDto {
    private Long bookingId;
    private String ticketId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String usn;
    private Long eventId;
    private String eventTitle;
    private LocalDateTime bookedAt;
    private String bookingStatus;
    private boolean paid;
    private double amountPaid;
    private boolean attended;
    private String attendanceStatus; // "ATTENDED" or "NOT_ATTENDED"
    private LocalDateTime checkedInAt;
}
