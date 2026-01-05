package com.college.bookmyslot.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class PublicEventResponse {

        private Long eventId;
        private String title;
        private String posterUrl;
        private String description;
        private LocalDate eventDate;
        private String location;

    }


