package com.college.bookmyslot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Unit tests for EmailService (Google Apps Script provider).
 * Uses MockRestServiceServer — no real HTTP traffic, no real emails sent.
 */
class EmailServiceTest {

    private static final String FAKE_SCRIPT_URL = "https://script.google.com/macros/s/fake/exec";
    private static final String FAKE_SECRET     = "test-secret-abc";

    /** Apps Script success response */
    private static final String SUCCESS_BODY = "{\"success\":true}";
    /** Apps Script failure response */
    private static final String FAILURE_BODY = "{\"success\":false,\"error\":\"quota exceeded\"}";

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer   = MockRestServiceServer.createServer(restTemplate);
        // Use the package-private constructor that accepts a pre-built RestTemplate
        emailService = new EmailService(restTemplate);
        // Inject @Value fields without starting a Spring context
        ReflectionTestUtils.setField(emailService, "scriptUrl", FAKE_SCRIPT_URL);
        ReflectionTestUtils.setField(emailService, "secret",    FAKE_SECRET);
    }

    // ------------------------------------------------------------------
    // Successful delivery — all 7 public methods
    // ------------------------------------------------------------------

    @Test
    void sendOtpEmail_success_postsToScriptUrl() {
        mockServer.expect(requestTo(FAKE_SCRIPT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        emailService.sendOtpEmail("user@test.com", "Alice", "111111", "Login");

        mockServer.verify();
    }

    @Test
    void sendWelcomeEmail_success_postsToScriptUrl() {
        mockServer.expect(requestTo(FAKE_SCRIPT_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        emailService.sendWelcomeEmail("user@test.com", "Alice", "STUDENT", "CS21CS001");

        mockServer.verify();
    }

    @Test
    void sendSlotBookingEmail_success_postsToScriptUrl() {
        mockServer.expect(requestTo(FAKE_SCRIPT_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        emailService.sendSlotBookingEmail(
                "user@test.com", "Alice", "Prof. Smith", "2026-10-15", "10:00", "11:00");

        mockServer.verify();
    }

    @Test
    void sendEventBookingEmail_success_postsToScriptUrl() {
        mockServer.expect(requestTo(FAKE_SCRIPT_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        emailService.sendEventBookingEmail(
                "user@test.com", "Alice", "Tech Fest", "Auditorium",
                "2026-10-20", "09:00", "TKT-001");

        mockServer.verify();
    }

    @Test
    void sendEventReminderEmail_success_postsToScriptUrl() {
        mockServer.expect(requestTo(FAKE_SCRIPT_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        emailService.sendEventReminderEmail(
                "user@test.com", "Alice", "Tech Fest", "Auditorium", "09:00 AM");

        mockServer.verify();
    }

    @Test
    void sendTeacherSlotBookedEmail_success_postsToScriptUrl() {
        mockServer.expect(requestTo(FAKE_SCRIPT_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        emailService.sendTeacherSlotBookedEmail(
                "teacher@test.com", "Prof. Smith", "Alice",
                "alice@test.com", "CS21CS001",
                "2026-10-15", "10:00", "11:00");

        mockServer.verify();
    }

    @Test
    void sendClubStaffCreatedEmail_success_postsToScriptUrl() {
        mockServer.expect(requestTo(FAKE_SCRIPT_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(SUCCESS_BODY, MediaType.APPLICATION_JSON));

        emailService.sendClubStaffCreatedEmail(
                "staff@test.com", "Bob", "Coding Club", "tempPass@123");

        mockServer.verify();
    }

    // ------------------------------------------------------------------
    // Apps Script application-level failure  { "success": false }
    // ------------------------------------------------------------------

    @Test
    void sendOtpEmail_scriptReportsFailure_throwsRuntimeException() {
        mockServer.expect(requestTo(FAKE_SCRIPT_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(FAILURE_BODY, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() ->
                emailService.sendOtpEmail("user@test.com", "Alice", "999", "Login"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Apps Script reported failure");
    }

    // ------------------------------------------------------------------
    // HTTP error handling
    // ------------------------------------------------------------------

    @Test
    void sendOtpEmail_script401_throwsHttpClientError() {
        mockServer.expect(requestTo(FAKE_SCRIPT_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":\"wrong secret\"}"));

        assertThatThrownBy(() ->
                emailService.sendOtpEmail("user@test.com", "Alice", "999", "Login"))
                .isInstanceOf(org.springframework.web.client.HttpClientErrorException.class);

        mockServer.verify();
    }

    @Test
    void sendOtpEmail_script403_throwsHttpClientError() {
        mockServer.expect(requestTo(FAKE_SCRIPT_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() ->
                emailService.sendOtpEmail("user@test.com", "Alice", "999", "Login"))
                .isInstanceOf(org.springframework.web.client.HttpClientErrorException.class);

        mockServer.verify();
    }

    @Test
    void sendOtpEmail_script500_throwsHttpServerError() {
        mockServer.expect(requestTo(FAKE_SCRIPT_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() ->
                emailService.sendOtpEmail("user@test.com", "Alice", "999", "Login"))
                .isInstanceOf(org.springframework.web.client.HttpServerErrorException.class);

        mockServer.verify();
    }

    @Test
    void sendOtpEmail_networkTimeout_throwsResourceAccessException() {
        mockServer.expect(requestTo(FAKE_SCRIPT_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(
                        new java.net.SocketTimeoutException("Connect timed out")));

        assertThatThrownBy(() ->
                emailService.sendOtpEmail("user@test.com", "Alice", "999", "Login"))
                .isInstanceOf(org.springframework.web.client.ResourceAccessException.class);

        mockServer.verify();
    }

    // ------------------------------------------------------------------
    // Missing configuration
    // ------------------------------------------------------------------

    @Test
    void sendOtpEmail_missingScriptUrl_throwsIllegalState() {
        ReflectionTestUtils.setField(emailService, "scriptUrl", "");

        assertThatThrownBy(() ->
                emailService.sendOtpEmail("user@test.com", "Alice", "999", "Login"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GMAIL_SCRIPT_URL");
    }

    @Test
    void sendOtpEmail_missingSecret_throwsIllegalState() {
        ReflectionTestUtils.setField(emailService, "secret", "");

        assertThatThrownBy(() ->
                emailService.sendOtpEmail("user@test.com", "Alice", "999", "Login"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOKMYSLOT_SECRET");
    }
}
