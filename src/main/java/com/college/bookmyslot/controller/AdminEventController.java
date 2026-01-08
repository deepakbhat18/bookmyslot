package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.*;
import com.college.bookmyslot.model.Club;
import com.college.bookmyslot.model.Event;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.ClubRepository;
import com.college.bookmyslot.repository.EventRepository;
import com.college.bookmyslot.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/admin/events")
//@CrossOrigin(origins = "*")
public class AdminEventController {

    private final EventRepository eventRepository;

    public AdminEventController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping("/pending")
    public List<EventResponse> pendingEvents() {

        return eventRepository.findByStatus(Event.Status.DRAFT)
                .stream()
                .map(event -> {
                    EventResponse r = new EventResponse();
                    r.setEventId(event.getId());
                    r.setTitle(event.getTitle());
                    r.setVenue(event.getVenue());
                    r.setEventDate(event.getEventDate().toString());
                    r.setStartTime(event.getStartTime().toString());
                    r.setEndTime(event.getEndTime().toString());
                    r.setClubName(event.getClub().getName());
                    return r;
                })
                .toList();
    }


    @PutMapping("/{eventId}/approve")
    public ApiResponse<String> approveEvent(@PathVariable Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setStatus(Event.Status.PUBLISHED);
        eventRepository.save(event);

        return new ApiResponse<>(true, "Event approved & published", null);
    }


    @PutMapping("/{eventId}/reject")
    public ApiResponse<String> rejectEvent(@PathVariable Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setStatus(Event.Status.CANCELLED);
        eventRepository.save(event);

        return new ApiResponse<>(true, "Event rejected", null);
    }
}
