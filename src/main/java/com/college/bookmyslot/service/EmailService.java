package com.college.bookmyslot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * Sends transactional emails via a Google Apps Script web-app endpoint.
 * All delivery uses outbound HTTPS on port 443 — no SMTP connections.
 *
 * Required environment variables:
 *   GMAIL_SCRIPT_URL     – Deployed Apps Script URL (https://script.google.com/macros/s/.../exec)
 *   BOOKMYSLOT_SECRET    – Shared secret validated by the Apps Script to reject unauthorised callers
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final RestTemplate restTemplate;

    @Value("${gmail.script.url:}")
    private String scriptUrl;

    @Value("${bookmyslot.secret:}")
    private String secret;

    /**
     * Primary constructor used by Spring. RestTemplateBuilder is auto-configured
     * by spring-boot-starter-web — no additional @Bean definition is required.
     */
    @Autowired
    public EmailService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Package-private constructor used only by unit tests (passes a pre-built RestTemplate). */
    EmailService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // -----------------------------------------------------------------------
    // Internal helper
    // -----------------------------------------------------------------------

    /**
     * POSTs a plain-text email to the configured Google Apps Script endpoint.
     *
     * Request body (JSON):
     *   { "secret": "...", "to": "...", "subject": "...", "body": "..." }
     *
     * Expected success response:
     *   { "success": true }
     *
     * @throws IllegalStateException   if GMAIL_SCRIPT_URL or BOOKMYSLOT_SECRET is not configured
     * @throws ResourceAccessException on network / timeout errors (retried by @Retryable callers)
     * @throws RuntimeException        when the script signals failure or returns an unexpected response
     */
    private void sendEmail(String to, String subject, String text) {
        if (scriptUrl == null || scriptUrl.isBlank()) {
            throw new IllegalStateException(
                    "GMAIL_SCRIPT_URL is not configured. Email cannot be sent.");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "BOOKMYSLOT_SECRET is not configured. Email cannot be sent.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // The 'secret' field authenticates this request to the Apps Script.
        // Never log the secret value.
        Map<String, String> body = Map.of(
                "secret",  secret,
                "to",      to,
                "subject", subject,
                "body",    text
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(scriptUrl, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                // Guard — RestTemplate normally throws on non-2xx, but handle defensively.
                throw new RuntimeException(
                        "Apps Script returned HTTP " + response.getStatusCode()
                        + " for recipient " + to);
            }

            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException(
                        "Apps Script returned an empty response for recipient " + to);
            }

            Object success = responseBody.get("success");
            if (!Boolean.TRUE.equals(success)) {
                // Apps Script returned { "success": false } or an unexpected payload.
                // Extract an error message if present; do not log sensitive content.
                Object errorMsg = responseBody.get("error");
                throw new RuntimeException(
                        "Apps Script reported failure sending to " + to
                        + (errorMsg != null ? ": " + errorMsg : ""));
            }

            log.info("Email sent via Apps Script to {}", to);

        } catch (HttpClientErrorException ex) {
            // 4xx from the Apps Script host (e.g. 401 = wrong secret, 403 = forbidden)
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                log.error("Apps Script rejected the request ({}). Check BOOKMYSLOT_SECRET and script permissions.",
                        ex.getStatusCode());
                // Auth failures should not be retried — re-throw as-is so @Retryable callers
                // see the exception but the 4xx means no point hammering the endpoint.
            } else {
                log.error("Apps Script HTTP client error {} for recipient {}: {}",
                        ex.getStatusCode(), to, ex.getResponseBodyAsString());
            }
            throw ex;   // propagate to @Retryable / @Recover

        } catch (HttpServerErrorException ex) {
            // 5xx — transient server error, worth retrying
            log.warn("Apps Script HTTP server error {} for recipient {}. Will retry.",
                    ex.getStatusCode(), to);
            throw ex;

        }
        // ResourceAccessException (connect / read timeout) propagates automatically
        // and is retried by the @Retryable annotation on each public method.
    }

    // -----------------------------------------------------------------------
    // Public email methods — signatures, templates, and behaviour unchanged
    // -----------------------------------------------------------------------

    @Async
    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void sendWelcomeEmail(String to, String name, String role, String usn) {
        StringBuilder text = new StringBuilder();
        text.append("Hi ").append(name).append(",\n\n");
        text.append("Your BookMySlot account has been created successfully.\n");
        text.append("Role: ").append(role).append("\n");
        if (usn != null && !usn.isBlank()) {
            text.append("USN: ").append(usn).append("\n");
        }
        text.append("\nRegards,\nBookMySlot Team");

        sendEmail(to, "Welcome to BookMySlot", text.toString());
    }

    @Async
    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void sendOtpEmail(String to, String name, String otp, String purpose) {
        String text =
                "Hi " + name + ",\n\n" +
                "Your OTP for " + purpose + " is: " + otp + "\n\n" +
                "This OTP is valid for 10 minutes.\n\n" +
                "Regards,\nBookMySlot Team";

        sendEmail(to, "BookMySlot OTP - " + purpose, text);
    }

    @Async
    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void sendSlotBookingEmail(
            String to,
            String name,
            String teacherName,
            String date,
            String startTime,
            String endTime
    ) {
        String text =
                "Hi " + name + ",\n\n" +
                "Your slot has been booked with " + teacherName + ".\n" +
                "Date: " + date + "\n" +
                "Time: " + startTime + " - " + endTime + "\n\n" +
                "Regards,\nBookMySlot Team";

        sendEmail(to, "Slot Booking Confirmation", text);
    }

    @Async
    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void sendTeacherSlotBookedEmail(
            String to,
            String teacherName,
            String studentName,
            String studentEmail,
            String studentUsn,
            String date,
            String startTime,
            String endTime
    ) {
        String text =
                "Hi " + teacherName + ",\n\n" +
                "A student has booked one of your slots.\n\n" +
                "Student: " + studentName + "\n" +
                "Email: " + studentEmail + "\n" +
                (studentUsn != null && !studentUsn.isBlank()
                        ? "USN: " + studentUsn + "\n"
                        : "") +
                "\nDate: " + date + "\n" +
                "Time: " + startTime + " - " + endTime + "\n\n" +
                "Regards,\nBookMySlot Team";

        sendEmail(to, "New slot booking from " + studentName, text);
    }

    @Async
    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void sendEventBookingEmail(
            String to,
            String studentName,
            String eventTitle,
            String venue,
            String eventDate,
            String eventTime,
            String ticketId
    ) {
        String text =
                "Hi " + studentName + ",\n\n" +
                "Your booking is confirmed 🎉\n\n" +
                "Event: " + eventTitle + "\n" +
                "Venue: " + venue + "\n" +
                "Date: " + eventDate + "\n" +
                "Time: " + eventTime + "\n" +
                "Ticket ID: " + ticketId + "\n\n" +
                "Regards,\nBookMySlot Team";

        sendEmail(to, "Event Booking Confirmed - " + eventTitle, text);
    }

    @Async
    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void sendEventReminderEmail(
            String to,
            String studentName,
            String eventTitle,
            String venue,
            String startTime
    ) {
        String text =
                "Hi " + studentName + ",\n\n" +
                "Reminder: \"" + eventTitle + "\" starts at " + startTime + ".\n\n" +
                "Venue: " + venue + "\n\n" +
                "— BookMySlot Team";

        sendEmail(to, "⏰ Reminder: Event starts soon", text);
    }

    @Async
    public void sendClubStaffCreatedEmail(
            String to,
            String name,
            String clubName,
            String password
    ) {
        String text =
                "Hi " + name + ",\n\n" +
                "You have been added as a Club Staff for:\n" +
                "Club: " + clubName + "\n\n" +
                "Login credentials:\n" +
                "Email: " + to + "\n" +
                "Temporary Password: " + password + "\n\n" +
                "Please login and reset your password immediately.\n\n" +
                "Regards,\nBookMySlot Team";

        sendEmail(to, "Club Staff Account Created - BookMySlot", text);
    }

    @Recover
    public void recover(Exception ex, String to, String name, String otp, String purpose) {
        System.err.println("Email permanently failed for: " + to);
        ex.printStackTrace();
    }
}
