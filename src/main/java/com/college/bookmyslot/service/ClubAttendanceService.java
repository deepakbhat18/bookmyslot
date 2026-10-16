package com.college.bookmyslot.service;

import com.college.bookmyslot.dto.ClubStaffEventOptionDto;
import com.college.bookmyslot.dto.EventRegistrationDto;
import com.college.bookmyslot.dto.EventRegistrationsSummaryResponse;
import com.college.bookmyslot.model.Club;
import com.college.bookmyslot.model.Event;
import com.college.bookmyslot.model.EventBooking;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.EventBookingRepository;
import com.college.bookmyslot.repository.EventRepository;
import com.college.bookmyslot.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ClubAttendanceService {

    private final EventRepository eventRepository;
    private final EventBookingRepository eventBookingRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ClubAttendanceService(
            EventRepository eventRepository,
            EventBookingRepository eventBookingRepository,
            UserRepository userRepository
    ) {
        this.eventRepository = eventRepository;
        this.eventBookingRepository = eventBookingRepository;
        this.userRepository = userRepository;
    }

    /**
     * Validates that the user is an active Club Staff member associated with a valid club.
     */
    public User getAuthorizedClubStaff(Long staffUserId) {
        if (staffUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }

        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Staff not found"));

        if (!staff.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }

        if (staff.getRole() != User.Role.CLUB) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized: Only club staff can access this");
        }

        if (staff.getClub() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff is not assigned to any club");
        }

        return staff;
    }

    /**
     * Validates that the event exists and belongs to the staff member's club.
     */
    public Event getAuthorizedEvent(Long eventId, User staff) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        Club eventClub = event.getClub();
        if (eventClub == null || !eventClub.getId().equals(staff.getClub().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized: Event does not belong to your club");
        }

        return event;
    }

    /**
     * Retrieves all events for the staff member's club.
     */
    @Transactional(readOnly = true)
    public List<ClubStaffEventOptionDto> getClubEventsForStaff(Long staffUserId) {
        User staff = getAuthorizedClubStaff(staffUserId);

        List<Event> events = eventRepository.findByClubOrderByEventDateDesc(staff.getClub());

        return events.stream()
                .map(e -> ClubStaffEventOptionDto.builder()
                        .eventId(e.getId())
                        .title(e.getTitle())
                        .eventDate(e.getEventDate())
                        .venue(e.getVenue())
                        .status(e.getStatus() != null ? e.getStatus().name() : "DRAFT")
                        .bookedSlots(e.getBookedSlots())
                        .totalSlots(e.getTotalSlots())
                        .build())
                .toList();
    }

    /**
     * Retrieves the registrations and attendance summary for a club event.
     */
    @Transactional(readOnly = true)
    public EventRegistrationsSummaryResponse getEventRegistrations(Long eventId, Long staffUserId) {
        User staff = getAuthorizedClubStaff(staffUserId);
        Event event = getAuthorizedEvent(eventId, staff);

        List<EventBooking> bookings = eventBookingRepository.findByEventIdWithStudentAndEvent(eventId);

        long attendedCount = bookings.stream().filter(EventBooking::isCheckedIn).count();
        long totalRegistrations = bookings.size();
        long notAttendedCount = totalRegistrations - attendedCount;

        List<EventRegistrationDto> dtos = bookings.stream()
                .map(this::mapToRegistrationDto)
                .toList();

        return EventRegistrationsSummaryResponse.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .eventDate(event.getEventDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .venue(event.getVenue())
                .status(event.getStatus() != null ? event.getStatus().name() : "")
                .totalRegistrations(totalRegistrations)
                .attendedCount(attendedCount)
                .notAttendedCount(notAttendedCount)
                .registrations(dtos)
                .build();
    }

    /**
     * Marks a registered student as attended.
     * Transactional and idempotent against duplicate requests.
     */
    @Transactional
    public EventRegistrationDto markAttendance(Long eventId, Long bookingId, Long staffUserId) {
        User staff = getAuthorizedClubStaff(staffUserId);
        Event event = getAuthorizedEvent(eventId, staff);

        EventBooking booking = eventBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));

        if (!booking.getEvent().getId().equals(event.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration does not belong to the selected event");
        }

        if (booking.getBookingStatus() == EventBooking.BookingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot mark attendance for a cancelled booking");
        }

        // Idempotency: if already marked attended, safely return without changing the timestamp
        if (!booking.isCheckedIn()) {
            booking.setCheckedIn(true);
            booking.setCheckedInAt(LocalDateTime.now());
            booking = eventBookingRepository.save(booking);
        }

        return mapToRegistrationDto(booking);
    }

    /**
     * Exports registrations to an Excel (.xlsx) file.
     * Neutralizes formula injection on all user-controlled text cells.
     */
    @Transactional(readOnly = true)
    public byte[] exportRegistrationsToExcel(Long eventId, Long staffUserId, String search, String statusFilter) {
        User staff = getAuthorizedClubStaff(staffUserId);
        Event event = getAuthorizedEvent(eventId, staff);

        List<EventBooking> bookings = eventBookingRepository.findByEventIdWithStudentAndEvent(eventId);

        // Apply search and status filter if provided
        List<EventRegistrationDto> dtos = new ArrayList<>();
        String searchLower = (search != null) ? search.trim().toLowerCase(Locale.ROOT) : "";
        String statusUpper = (statusFilter != null) ? statusFilter.trim().toUpperCase(Locale.ROOT) : "ALL";

        for (EventBooking booking : bookings) {
            EventRegistrationDto dto = mapToRegistrationDto(booking);

            // Filter by search (name, USN, email)
            if (!searchLower.isEmpty()) {
                boolean nameMatch = dto.getStudentName() != null && dto.getStudentName().toLowerCase(Locale.ROOT).contains(searchLower);
                boolean usnMatch = dto.getUsn() != null && dto.getUsn().toLowerCase(Locale.ROOT).contains(searchLower);
                boolean emailMatch = dto.getStudentEmail() != null && dto.getStudentEmail().toLowerCase(Locale.ROOT).contains(searchLower);
                if (!nameMatch && !usnMatch && !emailMatch) {
                    continue;
                }
            }

            // Filter by attendance status
            if ("ATTENDED".equals(statusUpper) && !dto.isAttended()) {
                continue;
            } else if ("NOT_ATTENDED".equals(statusUpper) && dto.isAttended()) {
                continue;
            }

            dtos.add(dto);
        }

        return generateExcelWorkbook(event, dtos);
    }

    private byte[] generateExcelWorkbook(Event event, List<EventRegistrationDto> registrations) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Registrations");
            sheet.setDisplayGridlines(true);

            // Header Style
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setCellBorders(headerStyle);

            // Data Cell Styles
            CellStyle standardStyle = workbook.createCellStyle();
            setCellBorders(standardStyle);

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            setCellBorders(centerStyle);

            CellStyle attendedStyle = workbook.createCellStyle();
            attendedStyle.setAlignment(HorizontalAlignment.CENTER);
            Font greenFont = workbook.createFont();
            greenFont.setBold(true);
            greenFont.setColor(IndexedColors.GREEN.getIndex());
            attendedStyle.setFont(greenFont);
            setCellBorders(attendedStyle);

            CellStyle notAttendedStyle = workbook.createCellStyle();
            notAttendedStyle.setAlignment(HorizontalAlignment.CENTER);
            Font redFont = workbook.createFont();
            redFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            notAttendedStyle.setFont(redFont);
            setCellBorders(notAttendedStyle);

            // Headers
            String[] headers = {
                    "Sl. No.",
                    "Student Name",
                    "USN",
                    "Student Email",
                    "Event Name",
                    "Registration Date",
                    "Booking Status",
                    "Attendance Status",
                    "Check-In Time"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill rows
            int rowIdx = 1;
            for (EventRegistrationDto reg : registrations) {
                Row row = sheet.createRow(rowIdx);
                row.setHeightInPoints(20);

                // Sl. No.
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowIdx);
                cell0.setCellStyle(centerStyle);

                // Student Name
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(sanitizeForFormulaInjection(reg.getStudentName()));
                cell1.setCellStyle(standardStyle);

                // USN
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(sanitizeForFormulaInjection(reg.getUsn() != null ? reg.getUsn() : "N/A"));
                cell2.setCellStyle(centerStyle);

                // Student Email
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(sanitizeForFormulaInjection(reg.getStudentEmail()));
                cell3.setCellStyle(standardStyle);

                // Event Name
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(sanitizeForFormulaInjection(event.getTitle()));
                cell4.setCellStyle(standardStyle);

                // Registration Date
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(reg.getBookedAt() != null ? reg.getBookedAt().format(DATE_TIME_FORMATTER) : "N/A");
                cell5.setCellStyle(centerStyle);

                // Booking Status
                Cell cell6 = row.createCell(6);
                cell6.setCellValue(sanitizeForFormulaInjection(reg.getBookingStatus()));
                cell6.setCellStyle(centerStyle);

                // Attendance Status
                Cell cell7 = row.createCell(7);
                if (reg.isAttended()) {
                    cell7.setCellValue("Attended");
                    cell7.setCellStyle(attendedStyle);
                } else {
                    cell7.setCellValue("Not Attended");
                    cell7.setCellStyle(notAttendedStyle);
                }

                // Check-In Time
                Cell cell8 = row.createCell(8);
                cell8.setCellValue(reg.getCheckedInAt() != null ? reg.getCheckedInAt().format(DATE_TIME_FORMATTER) : "-");
                cell8.setCellStyle(centerStyle);

                rowIdx++;
            }

            // Auto-size columns with sensible padding
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.max(currentWidth + 1200, 3500));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate Excel file: " + e.getMessage());
        }
    }

    private void setCellBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    /**
     * Prevents CSV / Excel Formula Injection.
     * If user-provided string starts with '=', '+', '-', '@', tab, or carriage return,
     * prefix with a single quote so spreadsheet engines interpret it strictly as plain text.
     */
    public String sanitizeForFormulaInjection(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") ||
                trimmed.startsWith("@") || trimmed.startsWith("\t") || trimmed.startsWith("\r")) {
            return "'" + trimmed;
        }
        return trimmed;
    }

    private EventRegistrationDto mapToRegistrationDto(EventBooking booking) {
        User student = booking.getStudent();
        Event event = booking.getEvent();

        return EventRegistrationDto.builder()
                .bookingId(booking.getId())
                .ticketId(booking.getTicketId())
                .studentId(student != null ? student.getId() : null)
                .studentName(student != null ? student.getName() : "Unknown")
                .studentEmail(student != null ? student.getEmail() : "Unknown")
                .usn(student != null ? student.getUsn() : null)
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .bookedAt(booking.getBookedAt())
                .bookingStatus(booking.getBookingStatus() != null ? booking.getBookingStatus().name() : "CONFIRMED")
                .paid(booking.isPaid())
                .amountPaid(booking.getAmountPaid())
                .attended(booking.isCheckedIn())
                .attendanceStatus(booking.isCheckedIn() ? "ATTENDED" : "NOT_ATTENDED")
                .checkedInAt(booking.getCheckedInAt())
                .build();
    }
}
