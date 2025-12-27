package com.college.bookmyslot.dto;

import lombok.Data;

@Data
public class EventListResponse {

    private Long eventId;
    private String title;
    private String venue;
    private String eventDate;
    private String startTime;

    private boolean paid;
    private double price;

    private int availableSeats;
    private String posterUrl;


}
