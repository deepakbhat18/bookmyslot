package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.PublicEventDetailResponse;
import com.college.bookmyslot.dto.PublicEventResponse;
import com.college.bookmyslot.model.Event;
import com.college.bookmyslot.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/public/events")
//@CrossOrigin("*")
public class PublicEventController {

    private final EventRepository eventRepository;

    public PublicEventController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping
    public Page<PublicEventResponse> getPublicEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        return eventRepository
                .findByStatusOrderByEventDateAsc(Event.Status.PUBLISHED, pageable)
                .map(this::mapToPublicListResponse);
    }

    @GetMapping("/{eventId}")
    public PublicEventDetailResponse getEventDetails(@PathVariable Long eventId) {

        Event event = eventRepository.findById(eventId)
                .filter(e -> e.getStatus() == Event.Status.PUBLISHED)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        return mapToPublicDetailResponse(event);
    }

    private PublicEventResponse mapToPublicListResponse(Event event) {
        PublicEventResponse dto = new PublicEventResponse();
        dto.setEventId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setPosterUrl(event.getPosterUrl());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());
        dto.setLocation(event.getVenue());
        return dto;
    }

    private PublicEventDetailResponse mapToPublicDetailResponse(Event event) {
        PublicEventDetailResponse dto = new PublicEventDetailResponse();
        dto.setEventId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setPosterUrl(event.getPosterUrl());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());
        dto.setStartTime(event.getStartTime());
        dto.setEndTime(event.getEndTime());
        dto.setVenue(event.getVenue());
        dto.setClubName(event.getClub().getName());
        dto.setEventType(event.getEventType().name());
        return dto;
    }
}
