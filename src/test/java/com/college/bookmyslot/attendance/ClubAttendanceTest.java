package com.college.bookmyslot.attendance;

import com.college.bookmyslot.controller.ClubAttendanceController;
import com.college.bookmyslot.dto.EventRegistrationDto;
import com.college.bookmyslot.dto.EventRegistrationsSummaryResponse;
import com.college.bookmyslot.interceptor.SessionAuthInterceptor;
import com.college.bookmyslot.model.Club;
import com.college.bookmyslot.model.Event;
import com.college.bookmyslot.model.EventBooking;
import com.college.bookmyslot.model.User;
import com.college.bookmyslot.repository.EventBookingRepository;
import com.college.bookmyslot.repository.EventRepository;
import com.college.bookmyslot.repository.UserRepository;
import com.college.bookmyslot.service.ClubAttendanceService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClubAttendanceTest {

    private MockMvc mockMvc;
    private EventRepository eventRepository;
    private EventBookingRepository bookingRepository;
    private UserRepository userRepository;
    private ClubAttendanceService attendanceService;
    private ClubAttendanceController attendanceController;
    private SessionAuthInterceptor authInterceptor;

    private Club clubAlpha;
    private Club clubBeta;
    private User staffAlpha;
    private User staffBeta;
    private User studentUser;
    private Event eventAlpha;
    private Event eventBeta;
    private EventBooking booking1;

    @BeforeEach
    void setUp() {
        eventRepository = Mockito.mock(EventRepository.class);
        bookingRepository = Mockito.mock(EventBookingRepository.class);
        userRepository = Mockito.mock(UserRepository.class);

        attendanceService = new ClubAttendanceService(eventRepository, bookingRepository, userRepository);
        attendanceController = new ClubAttendanceController(attendanceService);
        authInterceptor = new SessionAuthInterceptor();

        mockMvc = MockMvcBuilders.standaloneSetup(attendanceController)
                .addMappedInterceptors(new String[]{"/api/**"}, authInterceptor)
                .build();

        // Setup Club Alpha & Staff Alpha
        clubAlpha = new Club();
        clubAlpha.setId(101L);
        clubAlpha.setName("Coding Club");

        staffAlpha = new User();
        staffAlpha.setId(1L);
        staffAlpha.setName("Alpha Staff");
        staffAlpha.setEmail("staff.alpha@college.edu");
        staffAlpha.setRole(User.Role.CLUB);
        staffAlpha.setClub(clubAlpha);
        staffAlpha.setActive(true);

        // Setup Club Beta & Staff Beta
        clubBeta = new Club();
        clubBeta.setId(102L);
        clubBeta.setName("Robotics Club");

        staffBeta = new User();
        staffBeta.setId(2L);
        staffBeta.setName("Beta Staff");
        staffBeta.setEmail("staff.beta@college.edu");
        staffBeta.setRole(User.Role.CLUB);
        staffBeta.setClub(clubBeta);
        staffBeta.setActive(true);

        // Setup Student
        studentUser = new User();
        studentUser.setId(501L);
        studentUser.setName("Alex Rivera");
        studentUser.setEmail("alex@student.college.edu");
        studentUser.setUsn("1MS21CS099");
        studentUser.setRole(User.Role.STUDENT);

        // Setup Events
        eventAlpha = new Event();
        org.springframework.test.util.ReflectionTestUtils.setField(eventAlpha, "id", 1001L);
        eventAlpha.setTitle("Web Development Bootcamp");
        eventAlpha.setClub(clubAlpha);
        eventAlpha.setEventDate(LocalDate.of(2026, 11, 1));
        eventAlpha.setStartTime(LocalTime.of(10, 0));
        eventAlpha.setEndTime(LocalTime.of(13, 0));
        eventAlpha.setVenue("Auditorium 1");
        eventAlpha.setStatus(Event.Status.PUBLISHED);

        eventBeta = new Event();
        org.springframework.test.util.ReflectionTestUtils.setField(eventBeta, "id", 1002L);
        eventBeta.setTitle("Drone Building Workshop");
        eventBeta.setClub(clubBeta);
        eventBeta.setEventDate(LocalDate.of(2026, 11, 2));
        eventBeta.setStatus(Event.Status.PUBLISHED);

        // Setup Booking for Event Alpha
        booking1 = new EventBooking();
        booking1.setId(5001L);
        booking1.setEvent(eventAlpha);
        booking1.setStudent(studentUser);
        booking1.setTicketId("TICKET-ALPHA-12345");
        booking1.setBookedAt(LocalDateTime.of(2026, 10, 10, 14, 30));
        booking1.setBookingStatus(EventBooking.BookingStatus.CONFIRMED);
        booking1.setCheckedIn(false);
        booking1.setCheckedInAt(null);

        // Repository mocks
        when(userRepository.findById(1L)).thenReturn(Optional.of(staffAlpha));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staffBeta));
        when(eventRepository.findById(1001L)).thenReturn(Optional.of(eventAlpha));
        when(eventRepository.findById(1002L)).thenReturn(Optional.of(eventBeta));
    }

    @Test
    @DisplayName("1. Club Staff can view registrations for their own club's event")
    void testClubStaffCanViewOwnClubEventRegistrations() throws Exception {
        when(bookingRepository.findByEventIdWithStudentAndEvent(1001L)).thenReturn(List.of(booking1));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 1L);
        session.setAttribute("ROLE", "CLUB");

        mockMvc.perform(get("/api/staff/attendance/events/1001/registrations")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value(1001L))
                .andExpect(jsonPath("$.data.totalRegistrations").value(1))
                .andExpect(jsonPath("$.data.attendedCount").value(0))
                .andExpect(jsonPath("$.data.notAttendedCount").value(1))
                .andExpect(jsonPath("$.data.registrations[0].studentName").value("Alex Rivera"))
                .andExpect(jsonPath("$.data.registrations[0].usn").value("1MS21CS099"))
                .andExpect(jsonPath("$.data.registrations[0].attendanceStatus").value("NOT_ATTENDED"));
    }

    @Test
    @DisplayName("2. Registrations from another club are inaccessible (403 Forbidden)")
    void testRegistrationsFromAnotherClubInaccessible() {
        // Staff Beta tries to view Event Alpha (owned by Club Alpha)
        assertThatThrownBy(() -> attendanceService.getEventRegistrations(1001L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 403);
    }

    @Test
    @DisplayName("3. An event with no registrations displays an empty state")
    void testEventWithNoRegistrationsDisplaysEmptyState() {
        when(bookingRepository.findByEventIdWithStudentAndEvent(1001L)).thenReturn(Collections.emptyList());

        EventRegistrationsSummaryResponse summary = attendanceService.getEventRegistrations(1001L, 1L);

        assertThat(summary.getTotalRegistrations()).isEqualTo(0);
        assertThat(summary.getAttendedCount()).isEqualTo(0);
        assertThat(summary.getNotAttendedCount()).isEqualTo(0);
        assertThat(summary.getRegistrations()).isEmpty();
    }

    @Test
    @DisplayName("4. Viewing or exporting registrations never marks anyone as attended")
    void testViewingOrExportingNeverChangesAttendance() {
        when(bookingRepository.findByEventIdWithStudentAndEvent(1001L)).thenReturn(List.of(booking1));

        // Initial state is not attended
        assertThat(booking1.isCheckedIn()).isFalse();

        // View registrations
        attendanceService.getEventRegistrations(1001L, 1L);
        assertThat(booking1.isCheckedIn()).isFalse();
        assertThat(booking1.getCheckedInAt()).isNull();

        // Export registrations to Excel
        byte[] excelBytes = attendanceService.exportRegistrationsToExcel(1001L, 1L, null, null);
        assertThat(excelBytes).isNotEmpty();
        assertThat(booking1.isCheckedIn()).isFalse();
        assertThat(booking1.getCheckedInAt()).isNull();

        // Verify save was never called during view/export
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Marking a checked-in student attended persists the status")
    void testMarkAttendancePersistsStatus() {
        when(bookingRepository.findById(5001L)).thenReturn(Optional.of(booking1));
        when(bookingRepository.save(any(EventBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        EventRegistrationDto result = attendanceService.markAttendance(1001L, 5001L, 1L);

        assertThat(result.isAttended()).isTrue();
        assertThat(result.getAttendanceStatus()).isEqualTo("ATTENDED");
        assertThat(result.getCheckedInAt()).isNotNull();

        verify(bookingRepository, times(1)).save(booking1);
        assertThat(booking1.isCheckedIn()).isTrue();
        assertThat(booking1.getCheckedInAt()).isNotNull();
    }

    @Test
    @DisplayName("6. Repeated attendance requests do not create duplicate records or re-save")
    void testRepeatedAttendanceRequestsAreIdempotent() {
        booking1.setCheckedIn(true);
        LocalDateTime originalTime = LocalDateTime.of(2026, 10, 16, 9, 0);
        booking1.setCheckedInAt(originalTime);

        when(bookingRepository.findById(5001L)).thenReturn(Optional.of(booking1));

        EventRegistrationDto result = attendanceService.markAttendance(1001L, 5001L, 1L);

        assertThat(result.isAttended()).isTrue();
        assertThat(result.getCheckedInAt()).isEqualTo(originalTime);

        // Did not re-save or duplicate
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Unauthorized users cannot mark attendance")
    void testUnauthorizedUserCannotMarkAttendance() {
        // Staff Beta (Club Beta) tries to mark attendance for Event Alpha (Club Alpha)
        assertThatThrownBy(() -> attendanceService.markAttendance(1001L, 5001L, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 403);

        // Student tries to mark attendance
        User studentAsUser = new User();
        studentAsUser.setId(99L);
        studentAsUser.setRole(User.Role.STUDENT);
        studentAsUser.setActive(true);
        when(userRepository.findById(99L)).thenReturn(Optional.of(studentAsUser));

        assertThatThrownBy(() -> attendanceService.markAttendance(1001L, 5001L, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 403);
    }

    @Test
    @DisplayName("8. A registration cannot be marked attended for the wrong event")
    void testRegistrationCannotBeMarkedForWrongEvent() {
        // Event Alpha has another event in same club
        Event eventAlpha2 = new Event();
        org.springframework.test.util.ReflectionTestUtils.setField(eventAlpha2, "id", 1003L);
        eventAlpha2.setTitle("Advanced Algorithms");
        eventAlpha2.setClub(clubAlpha);
        when(eventRepository.findById(1003L)).thenReturn(Optional.of(eventAlpha2));

        when(bookingRepository.findById(5001L)).thenReturn(Optional.of(booking1)); // booking1 belongs to eventAlpha (1001L)

        // Try marking booking1 under event 1003L
        assertThatThrownBy(() -> attendanceService.markAttendance(1003L, 5001L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 400);
    }

    @Test
    @DisplayName("9. Cannot mark attendance for cancelled booking")
    void testCannotMarkAttendanceForCancelledBooking() {
        booking1.setBookingStatus(EventBooking.BookingStatus.CANCELLED);
        when(bookingRepository.findById(5001L)).thenReturn(Optional.of(booking1));

        assertThatThrownBy(() -> attendanceService.markAttendance(1001L, 5001L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 400);
    }

    @Test
    @DisplayName("10. Excel export generates valid .xlsx with correct registrations and neutralizes formula injection")
    void testExcelExportGeneratesValidXlsxAndNeutralizesFormulaInjection() throws Exception {
        // Create a student whose name/USN contains formula injection attempts
        User maliciousStudent = new User();
        maliciousStudent.setId(502L);
        maliciousStudent.setName("=cmd|'/C calc'!A0");
        maliciousStudent.setUsn("+1MS21CS999");
        maliciousStudent.setEmail("@student@college.edu");

        EventBooking injectionBooking = new EventBooking();
        injectionBooking.setId(5002L);
        injectionBooking.setEvent(eventAlpha);
        injectionBooking.setStudent(maliciousStudent);
        injectionBooking.setTicketId("TICKET-INJECT-99");
        injectionBooking.setBookedAt(LocalDateTime.of(2026, 10, 11, 10, 0));
        injectionBooking.setBookingStatus(EventBooking.BookingStatus.CONFIRMED);
        injectionBooking.setCheckedIn(true);
        injectionBooking.setCheckedInAt(LocalDateTime.of(2026, 10, 11, 10, 30));

        when(bookingRepository.findByEventIdWithStudentAndEvent(1001L))
                .thenReturn(List.of(booking1, injectionBooking));

        byte[] excelBytes = attendanceService.exportRegistrationsToExcel(1001L, 1L, null, null);

        assertThat(excelBytes).isNotNull().isNotEmpty();

        // Read the generated Excel using Apache POI
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet("Registrations");
            assertThat(sheet).isNotNull();

            // Verify header row
            Row headerRow = sheet.getRow(0);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Sl. No.");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Student Name");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("USN");
            assertThat(headerRow.getCell(3).getStringCellValue()).isEqualTo("Student Email");
            assertThat(headerRow.getCell(4).getStringCellValue()).isEqualTo("Event Name");
            assertThat(headerRow.getCell(5).getStringCellValue()).isEqualTo("Registration Date");
            assertThat(headerRow.getCell(6).getStringCellValue()).isEqualTo("Booking Status");
            assertThat(headerRow.getCell(7).getStringCellValue()).isEqualTo("Attendance Status");
            assertThat(headerRow.getCell(8).getStringCellValue()).isEqualTo("Check-In Time");

            // Verify Row 1 (Normal student)
            Row row1 = sheet.getRow(1);
            assertThat(row1.getCell(1).getStringCellValue()).isEqualTo("Alex Rivera");
            assertThat(row1.getCell(2).getStringCellValue()).isEqualTo("1MS21CS099");
            assertThat(row1.getCell(7).getStringCellValue()).isEqualTo("Not Attended");

            // Verify Row 2 (Formula Injection neutralization check)
            Row row2 = sheet.getRow(2);
            // Must be prepended with ' so Excel interprets it safely as text
            assertThat(row2.getCell(1).getStringCellValue()).startsWith("'=");
            assertThat(row2.getCell(2).getStringCellValue()).startsWith("'+");
            assertThat(row2.getCell(3).getStringCellValue()).startsWith("'@");
            assertThat(row2.getCell(7).getStringCellValue()).isEqualTo("Attended");
        }
    }
}
