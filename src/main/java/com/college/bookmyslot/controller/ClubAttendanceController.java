package com.college.bookmyslot.controller;

import com.college.bookmyslot.dto.ApiResponse;
import com.college.bookmyslot.dto.ClubStaffEventOptionDto;
import com.college.bookmyslot.dto.EventRegistrationDto;
import com.college.bookmyslot.dto.EventRegistrationsSummaryResponse;
import com.college.bookmyslot.service.ClubAttendanceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/staff/attendance")
public class ClubAttendanceController {

    private final ClubAttendanceService clubAttendanceService;

    public ClubAttendanceController(ClubAttendanceService clubAttendanceService) {
        this.clubAttendanceService = clubAttendanceService;
    }

    private Long getAuthenticatedStaffUserId(HttpSession session) {
        if (session == null || session.getAttribute("USER_ID") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return (Long) session.getAttribute("USER_ID");
    }

    /**
     * Lists events belonging to the authenticated Club Staff member's club.
     */
    @GetMapping("/events")
    public ApiResponse<List<ClubStaffEventOptionDto>> getClubEvents(HttpSession session) {
        Long staffUserId = getAuthenticatedStaffUserId(session);
        List<ClubStaffEventOptionDto> events = clubAttendanceService.getClubEventsForStaff(staffUserId);

        return new ApiResponse<>(true, "Club events retrieved successfully", events);
    }

    /**
     * Retrieves registrations and attendance summary for a specific event belonging to staff's club.
     */
    @GetMapping("/events/{eventId}/registrations")
    public ApiResponse<EventRegistrationsSummaryResponse> getEventRegistrations(
            @PathVariable Long eventId,
            HttpSession session
    ) {
        Long staffUserId = getAuthenticatedStaffUserId(session);
        EventRegistrationsSummaryResponse response = clubAttendanceService.getEventRegistrations(eventId, staffUserId);

        return new ApiResponse<>(true, "Event registrations retrieved successfully", response);
    }

    /**
     * Explicitly marks a checked-in student as ATTENDED.
     */
    @PostMapping("/events/{eventId}/registrations/{bookingId}/mark-attended")
    public ApiResponse<EventRegistrationDto> markAttendance(
            @PathVariable Long eventId,
            @PathVariable Long bookingId,
            HttpSession session
    ) {
        Long staffUserId = getAuthenticatedStaffUserId(session);
        EventRegistrationDto response = clubAttendanceService.markAttendance(eventId, bookingId, staffUserId);

        return new ApiResponse<>(true, "Attendance marked successfully", response);
    }

    /**
     * Exports registrations for the selected event to an Excel (.xlsx) file.
     * Purely read-only operation.
     */
    @GetMapping("/events/{eventId}/export")
    public ResponseEntity<byte[]> exportRegistrations(
            @PathVariable Long eventId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            HttpSession session
    ) {
        Long staffUserId = getAuthenticatedStaffUserId(session);

        // Fetch event registrations summary for event title in filename
        EventRegistrationsSummaryResponse summary = clubAttendanceService.getEventRegistrations(eventId, staffUserId);

        byte[] excelBytes = clubAttendanceService.exportRegistrationsToExcel(eventId, staffUserId, search, status);

        // Format clean filename: e.g. "Hackathon_Registrations.xlsx"
        String rawTitle = summary.getEventTitle() != null ? summary.getEventTitle() : "Event";
        String safeTitle = rawTitle.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (safeTitle.length() > 50) {
            safeTitle = safeTitle.substring(0, 50);
        }
        String filename = safeTitle + "_Registrations.xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excelBytes.length);

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}
