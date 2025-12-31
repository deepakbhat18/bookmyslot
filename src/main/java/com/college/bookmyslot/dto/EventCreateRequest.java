package com.college.bookmyslot.dto;

import lombok.Data;

@Data
public class EventCreateRequest {

    private String title;
    private String description;
    private String venue;

    private String eventDate;

    private String startTime;
    private String endTime;

    private boolean paid;
    private double price;

    private int maxSeats;

}
