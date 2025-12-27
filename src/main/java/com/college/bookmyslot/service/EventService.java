package com.college.bookmyslot.service;

import com.college.bookmyslot.dto.EventCreateRequest;
import com.college.bookmyslot.dto.EventListResponse;
import com.college.bookmyslot.dto.EventResponse;
import com.college.bookmyslot.dto.EventUpdateRequest;
import com.college.bookmyslot.model.Club;
import com.college.bookmyslot.model.Event;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.ClubRepository;
import com.college.bookmyslot.repository.EventRepository;
import com.college.bookmyslot.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    public EventService(EventRepository eventRepository,
                        ClubRepository clubRepository,UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.clubRepository = clubRepository;
        this.userRepository=userRepository;
    }

    public EventResponse createEventByStaff(Long staffUserId, EventCreateRequest request) {

        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (staff.getRole() != User.Role.CLUB) {
            throw new RuntimeException("Only club staff can create events");
        }

        if (staff.getClub() == null) {
            throw new RuntimeException("Club staff is not linked to any club");
        }

        Club club = staff.getClub();

        if (club.getStatus() != Club.Status.ACTIVE) {
            throw new RuntimeException("Club is inactive. Cannot create events.");
        }

        LocalTime start = LocalTime.parse(request.getStartTime());
        LocalTime end = LocalTime.parse(request.getEndTime());

        if (!start.isBefore(end)) {
            throw new RuntimeException("Start time must be before end time");
        }

        Event event = new Event();
        event.setClub(club);
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setVenue(request.getVenue());
        event.setEventDate(LocalDate.parse(request.getEventDate()));
        event.setStartTime(start);
        event.setEndTime(end);
        event.setTotalSlots(request.getMaxSeats());
        event.setBookedSlots(0);
        event.setStatus(Event.Status.DRAFT);


        if (request.isPaid()) {
            event.setEventType(Event.EventType.PAID);
            event.setTicketPrice(request.getPrice());
        } else {
            event.setEventType(Event.EventType.FREE);
            event.setTicketPrice(0.0);
        }

        Event saved = eventRepository.save(event);
        return mapToEventResponse(saved);
    }


    public EventResponse updateEvent(Long eventId, EventUpdateRequest request) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setVenue(request.getVenue());

        event.setStartTime(LocalTime.parse(request.getStartTime()));
        event.setEndTime(LocalTime.parse(request.getEndTime()));

        event.setTotalSlots(request.getMaxSeats());

        if (request.isPaid()) {
            event.setEventType(Event.EventType.PAID);
            event.setTicketPrice(request.getPrice());
        } else {
            event.setEventType(Event.EventType.FREE);
            event.setTicketPrice(0.0);
        }

        Event updated = eventRepository.save(event);
        return mapToEventResponse(updated);
    }

    public List<EventListResponse> listEventsByDate(String date) {

        LocalDate eventDate = LocalDate.parse(date);

        return eventRepository
                .findByEventDateAndStatus(eventDate, Event.Status.PUBLISHED)
                .stream()
                .map(this::mapToEventListResponse)
                .collect(Collectors.toList());
    }


    public EventResponse getEventById(Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        return mapToEventResponse(event);
    }

    private EventResponse mapToEventResponse(Event event) {

        EventResponse r = new EventResponse();
        r.setEventId(event.getId());
        r.setTitle(event.getTitle());
        r.setDescription(event.getDescription());
        r.setVenue(event.getVenue());
        r.setEventDate(event.getEventDate().toString());
        r.setStartTime(event.getStartTime().toString());
        r.setEndTime(event.getEndTime().toString());
        r.setPaid(event.getEventType() == Event.EventType.PAID);
        r.setPrice(event.getTicketPrice());
        r.setTotalSeats(event.getTotalSlots());
        r.setAvailableSeats(event.getTotalSlots() - event.getBookedSlots());
        r.setClubName(event.getClub().getName());
       r.setPosterUrl(event.getPosterUrl());

        return r;
    }

    private EventListResponse mapToEventListResponse(Event event) {

        EventListResponse r = new EventListResponse();
        r.setEventId(event.getId());
        r.setTitle(event.getTitle());
        r.setVenue(event.getVenue());
        r.setEventDate(event.getEventDate().toString());
        r.setStartTime(event.getStartTime().toString());
        r.setPaid(event.getEventType() == Event.EventType.PAID);
        r.setPrice(event.getTicketPrice());
        r.setAvailableSeats(event.getTotalSlots() - event.getBookedSlots());
        r.setPosterUrl(event.getPosterUrl());
        return r;
    }
    public EventResponse getEventDetails(Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        return mapToEventResponse(event);
    }

}