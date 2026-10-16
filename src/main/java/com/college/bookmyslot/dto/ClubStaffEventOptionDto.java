package com.college.bookmyslot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubStaffEventOptionDto {
    private Long eventId;
    private String title;
    private LocalDate eventDate;
    private String venue;
    private String status;
    private int bookedSlots;
    private int totalSlots;
}
