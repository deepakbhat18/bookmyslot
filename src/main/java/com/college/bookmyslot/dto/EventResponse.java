package com.college.bookmyslot.dto;

import lombok.Data;

@Data
public class EventResponse {
    private Long eventId;
    private String title;
    private String description;
    private String venue;
    private String eventDate;
    private String startTime;
    private String endTime;
    private boolean paid;
    private double price;
    private int totalSeats;
    private int availableSeats;
    private String clubName;
    private String posterUrl;

}
