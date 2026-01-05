package com.college.bookmyslot.dto;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;
@Data
public class PublicEventDetailResponse {

    private Long eventId;
    private String title;
    private String posterUrl;
    private String description;

    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private String venue;
    private String clubName;
    private String eventType;

}
