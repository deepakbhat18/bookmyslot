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
 * Unit tests for EmailService.
 * Uses MockRestServiceServer to intercept RestTemplate calls — no real HTTP traffic or emails sent.
 */
class EmailServiceTest {

    private static final String FAKE_API_KEY   = "re_test_key";
    private static final String FAKE_FROM      = "noreply@example.com";
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer   = MockRestServiceServer.createServer(restTemplate);
        // Use the package-private constructor that accepts a pre-built RestTemplate
        emailService = new EmailService(restTemplate);
        // Inject @Value fields via ReflectionTestUtils (avoids Spring context overhead)
        ReflectionTestUtils.setField(emailService, "apiKey",    FAKE_API_KEY);
        ReflectionTestUtils.setField(emailService, "fromEmail", FAKE_FROM);
    }

    // ------------------------------------------------------------------
    // Successful delivery
    // ------------------------------------------------------------------

    @Test
    void sendOtpEmail_success_postsToResendApi() {
        mockServer.expect(requestTo(RESEND_API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + FAKE_API_KEY))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("{\"id\":\"abc123\"}", MediaType.APPLICATION_JSON));

        // Direct call (bypasses @Async for synchronous test)
        emailService.sendOtpEmail("user@test.com", "Alice", "111111", "Login");

        mockServer.verify();
    }

    @Test
    void sendWelcomeEmail_success_postsToResendApi() {
        mockServer.expect(requestTo(RESEND_API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"abc124\"}", MediaType.APPLICATION_JSON));

        emailService.sendWelcomeEmail("user@test.com", "Alice", "STUDENT", "CS21CS001");

        mockServer.verify();
    }

    @Test
    void sendSlotBookingEmail_success_postsToResendApi() {
        mockServer.expect(requestTo(RESEND_API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"abc125\"}", MediaType.APPLICATION_JSON));

        emailService.sendSlotBookingEmail(
                "user@test.com", "Alice", "Prof. Smith", "2026-10-15", "10:00", "11:00");

        mockServer.verify();
    }

    @Test
    void sendEventBookingEmail_success_postsToResendApi() {
        mockServer.expect(requestTo(RESEND_API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"abc126\"}", MediaType.APPLICATION_JSON));

        emailService.sendEventBookingEmail(
                "user@test.com", "Alice", "Tech Fest", "Auditorium",
                "2026-10-20", "09:00", "TKT-001");

        mockServer.verify();
    }

    @Test
    void sendEventReminderEmail_success_postsToResendApi() {
        mockServer.expect(requestTo(RESEND_API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"abc127\"}", MediaType.APPLICATION_JSON));

        emailService.sendEventReminderEmail(
                "user@test.com", "Alice", "Tech Fest", "Auditorium", "09:00 AM");

        mockServer.verify();
    }

    @Test
    void sendTeacherSlotBookedEmail_success_postsToResendApi() {
        mockServer.expect(requestTo(RESEND_API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"abc128\"}", MediaType.APPLICATION_JSON));

        emailService.sendTeacherSlotBookedEmail(
                "teacher@test.com", "Prof. Smith", "Alice",
                "alice@test.com", "CS21CS001",
                "2026-10-15", "10:00", "11:00");

        mockServer.verify();
    }

    @Test
    void sendClubStaffCreatedEmail_success_postsToResendApi() {
        mockServer.expect(requestTo(RESEND_API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":\"abc129\"}", MediaType.APPLICATION_JSON));

        emailService.sendClubStaffCreatedEmail(
                "staff@test.com", "Bob", "Coding Club", "tempPass@123");

        mockServer.verify();
    }

    // ------------------------------------------------------------------
    // Provider error handling
    // ------------------------------------------------------------------

    @Test
    void sendOtpEmail_resend401_throwsHttpClientError() {
        mockServer.expect(requestTo(RESEND_API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"message\":\"Invalid API key\"}"));

        assertThatThrownBy(() ->
                emailService.sendOtpEmail("user@test.com", "Alice", "999", "Login"))
                .isInstanceOf(org.springframework.web.client.HttpClientErrorException.class);

        mockServer.verify();
    }

    @Test
    void sendOtpEmail_resend500_throwsHttpServerError() {
        mockServer.expect(requestTo(RESEND_API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() ->
                emailService.sendOtpEmail("user@test.com", "Alice", "999", "Login"))
                .isInstanceOf(org.springframework.web.client.HttpServerErrorException.class);

        mockServer.verify();
    }

    @Test
    void sendOtpEmail_networkTimeout_throwsResourceAccessException() {
        mockServer.expect(requestTo(RESEND_API_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(
                        new java.net.SocketTimeoutException("Connect timed out")));

        assertThatThrownBy(() ->
                emailService.sendOtpEmail("user@test.com", "Alice", "999", "Login"))
                .isInstanceOf(org.springframework.web.client.ResourceAccessException.class);

        mockServer.verify();
    }

    // ------------------------------------------------------------------
    // Missing configuration handling
    // ------------------------------------------------------------------

    @Test
    void sendOtpEmail_missingApiKey_throwsIllegalState() {
        ReflectionTestUtils.setField(emailService, "apiKey", "");

        assertThatThrownBy(() ->
                emailService.sendOtpEmail("user@test.com", "Alice", "999", "Login"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESEND_API_KEY");
    }

    @Test
    void sendOtpEmail_missingFromEmail_throwsIllegalState() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "");

        assertThatThrownBy(() ->
                emailService.sendOtpEmail("user@test.com", "Alice", "999", "Login"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESEND_FROM_EMAIL");
    }
}
