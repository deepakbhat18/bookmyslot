package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.*;
import com.college.bookmyslot.model.Event;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.EventRepository;
import com.college.bookmyslot.repository.UserRepository;
import com.college.bookmyslot.service.EventService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.nio.file.*;

@RestController
@RequestMapping("/api/events")
//@CrossOrigin(origins = "*")
public class EventController {

    private final EventService eventService;

    private final EventRepository eventRepository;//
    private final UserRepository userRepository;

    public EventController(EventService eventService,
                           EventRepository eventRepository,UserRepository userRepository) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
        this.userRepository=userRepository;
    }

    @PostMapping
    public EventResponse createEvent(
            HttpSession session,
            @RequestBody EventCreateRequest request
    ) {
        if (session == null || session.getAttribute("USER_ID") == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Login required");
        }
        Long staffUserId = (Long) session.getAttribute("USER_ID");

        return eventService.createEventByStaff(staffUserId, request);
    }

    @GetMapping("/staff/{staffUserId}")
    public List<EventResponse> getEventsByStaff(
            @PathVariable Long staffUserId
    ) {
        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        return eventRepository.findByClub(staff.getClub())
                .stream()
                .map(event -> eventService.getEventDetails(event.getId()))
                .toList();
    }



    @PutMapping("/{eventId}")
    public EventResponse updateEvent(
            @PathVariable Long eventId,
            @RequestBody EventUpdateRequest request
    ) {
        return eventService.updateEvent(eventId, request);
    }

    @GetMapping
    public List<EventListResponse> getEventsByDate(
            @RequestParam String date
    ) {
        return eventService.listEventsByDate(date);
    }

    @GetMapping("/{eventId}")
    public EventResponse getEventDetails(
            @PathVariable Long eventId
    ) {
        return eventService.getEventDetails(eventId);
    }
    @PostMapping(
            value = "/{eventId}/poster",
            consumes = "multipart/form-data"
    )
    public ApiResponse<String> uploadPoster(
            @PathVariable Long eventId,
            @RequestParam("poster") MultipartFile poster
    ) throws IOException {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (poster.isEmpty()) {
            throw new RuntimeException("Poster file is required");
        }

        String fileName = "event_" + eventId + ".jpg";
        Path path = Paths.get("uploads/events/" + fileName);

        Files.createDirectories(path.getParent());
        Files.write(path, poster.getBytes());

        String posterUrl = "/uploads/events/" + fileName;
        event.setPosterUrl(posterUrl);
        eventRepository.save(event);

        return new ApiResponse<>(true, "Poster uploaded successfully", posterUrl);
    }


}
//@RestController
//@RequestMapping("/api/events")
//@CrossOrigin(origins = "*")
//public class EventController {
//
//    private final EventService eventService;
//    private final EventRepository eventRepository;
//
//    public EventController(EventService eventService,
//                           EventRepository eventRepository) {
//        this.eventService = eventService;
//        this.eventRepository = eventRepository;
//    }
//    @PostMapping
//    public EventResponse createEvent(
//            @RequestParam Long staffUserId,
//            @RequestBody EventCreateRequest request
//    ) {
//        return eventService.createEventByStaff(staffUserId, request);
//    }
//
//
//    @PutMapping("/{eventId}")
//    public EventResponse updateEvent(
//            @PathVariable Long eventId,
//            @RequestBody EventUpdateRequest request
//    ) {
//        return eventService.updateEvent(eventId, request);
//    }
//
//    @GetMapping
//    public List<EventListResponse> getEventsByDate(
//            @RequestParam String date
//    ) {
//        return eventService.listEventsByDate(date);
//    }
//
//    @GetMapping("/{eventId}")
//    public EventResponse getEventDetails(
//            @PathVariable Long eventId
//    ) {
//        return eventService.getEventDetails(eventId);
//    }
//
//    @PostMapping(
//            value = "/{eventId}/poster",
//            consumes = "multipart/form-data"
//    )
//    public ApiResponse<String> uploadPoster(
//            @PathVariable Long eventId,
//            @RequestPart("poster") MultipartFile poster
//    ) throws IOException {
//
//        Event event = eventRepository.findById(eventId)
//                .orElseThrow(() -> new RuntimeException("Event not found"));
//
//        if (poster.isEmpty()) {
//            throw new RuntimeException("Poster file is required");
//        }
//
//        String fileName = "event_" + eventId + ".jpg";
//        Path path = Paths.get("uploads/events/" + fileName);
//
//        Files.createDirectories(path.getParent());
//        Files.write(path, poster.getBytes());
//
//        String posterUrl = "/uploads/events/" + fileName;
//        event.setPosterUrl(posterUrl);
//        eventRepository.save(event);
//
//        return new ApiResponse<>(true, "Poster uploaded successfully", posterUrl);
//    }
//}
