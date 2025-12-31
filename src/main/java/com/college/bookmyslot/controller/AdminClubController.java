package com.college.bookmyslot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


import com.college.bookmyslot.dto.ApiResponse;
import com.college.bookmyslot.dto.ClubCreateRequest;
import com.college.bookmyslot.dto.ClubStaffCreateRequest;
import com.college.bookmyslot.dto.EventListResponse;
import com.college.bookmyslot.model.Club;
import com.college.bookmyslot.model.Event;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.ClubRepository;
import com.college.bookmyslot.repository.EventRepository;
import com.college.bookmyslot.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/clubs")
@CrossOrigin(origins = "*")
public class AdminClubController {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    public AdminClubController(ClubRepository clubRepository, UserRepository userRepository, EventRepository eventRepository ) {
        this.clubRepository = clubRepository;
        this.userRepository=userRepository;
        this.eventRepository = eventRepository;
    }


    @PostMapping
    public ApiResponse<Club> createClub(
            @RequestBody ClubCreateRequest request
    ) {

        if (clubRepository.existsByName(request.getName())) {
            throw new RuntimeException("Club name already exists");
        }

        if (clubRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Club email already exists");
        }

        Club club = new Club();
        club.setName(request.getName());
        club.setEmail(request.getEmail());
        club.setDescription(request.getDescription());

        Club saved = clubRepository.save(club);

        return new ApiResponse<>(
                true,
                "Club created successfully",
                saved
        );
    }
    @PostMapping("/staff")
    public ApiResponse<String> createClubStaff(
            @RequestBody ClubStaffCreateRequest request
    ) {
        Club club = clubRepository.findById(request.getClubId())
                .orElseThrow(() -> new RuntimeException("Club not found"));

        if (userRepository.existsByClub(club)) {
            throw new RuntimeException("This club already has a staff");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        User staff = new User();
        staff.setName(request.getName());
        staff.setEmail(request.getEmail());
        staff.setPassword(request.getPassword());
        staff.setRole(User.Role.CLUB);
        staff.setClub(club);
        staff.setVerified(true); // admin-created → auto verified

        userRepository.save(staff);

        return new ApiResponse<>(
                true,
                "Club staff created successfully",
                null
        );
    }

    @GetMapping
    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }

    @PutMapping("/{clubId}/status")
    public ApiResponse<String> updateClubStatus(
            @PathVariable Long clubId,
            @RequestParam Club.Status status
    ) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found"));

        club.setStatus(status);
        clubRepository.save(club);

        return new ApiResponse<>(
                true,
                "Club status updated to " + status,
                null
        );
    }


    @GetMapping("/{clubId}/events")
    public List<EventListResponse> getClubEvents(
            @PathVariable Long clubId
    ) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found"));

        return eventRepository.findByClub(club)
                .stream()
                .map(event -> {
                    EventListResponse r = new EventListResponse();
                    r.setEventId(event.getId());
                    r.setTitle(event.getTitle());
                    r.setVenue(event.getVenue());
                    r.setEventDate(event.getEventDate().toString());
                    r.setStartTime(event.getStartTime().toString());
                    r.setPaid(event.getEventType() == Event.EventType.PAID);
                    r.setPrice(event.getTicketPrice());
                    r.setAvailableSeats(
                            event.getTotalSlots() - event.getBookedSlots()
                    );
                    return r;
                })
                .toList();
    }
}
