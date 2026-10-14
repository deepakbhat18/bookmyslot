package com.college.bookmyslot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * Sends transactional emails via Resend's HTTPS API (POST https://api.resend.com/emails).
 * No SMTP connections are made; all delivery uses outbound HTTPS on port 443.
 *
 * Required environment variables:
 *   RESEND_API_KEY      – Resend API key (re_...)
 *   RESEND_FROM_EMAIL   – Verified sender address configured in Resend
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final RestTemplate restTemplate;

    @Value("${resend.api.key:}")
    private String apiKey;

    @Value("${resend.from.email:}")
    private String fromEmail;

    /**
     * RestTemplateBuilder is auto-configured by spring-boot-starter-web.
     * No additional @Bean definition is required.
     */
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
     * Sends a plain-text email via Resend API.
     *
     * @throws IllegalStateException    if RESEND_API_KEY or RESEND_FROM_EMAIL is not configured
     * @throws ResourceAccessException  on network / timeout errors (retryable)
     * @throws RuntimeException         when Resend rejects the request (non-retryable for 4xx)
     */
    private void sendEmail(String to, String subject, String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "RESEND_API_KEY is not configured. Email to " + to + " cannot be sent.");
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException(
                    "RESEND_FROM_EMAIL is not configured. Email to " + to + " cannot be sent.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);   // Authorization: Bearer <key>

        Map<String, Object> body = Map.of(
                "from", fromEmail,
                "to",   new String[]{to},
                "subject", subject,
                "text", text
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(RESEND_API_URL, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                // Should not reach here (RestTemplate throws on 4xx/5xx), but guard anyway
                throw new RuntimeException(
                        "Resend API returned unexpected status " + response.getStatusCode()
                        + " for recipient " + to);
            }
            log.info("Email sent via Resend to {}", to);

        } catch (HttpClientErrorException ex) {
            // 4xx: bad request, invalid key, unverified domain — do NOT retry
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                    ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.error("Resend API authentication failed. Check RESEND_API_KEY.");
            } else if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("Resend rate limit hit sending to {}. Will retry.", to);
                throw ex;   // allow @Retryable to kick in
            } else {
                log.error("Resend API client error {} sending to {}: {}",
                        ex.getStatusCode(), to, ex.getResponseBodyAsString());
            }
            // Re-throw so @Retryable / @Recover can handle appropriately
            throw ex;
        }
        // ResourceAccessException (connect / read timeout) propagates and is retried by @Retryable
    }

    // -----------------------------------------------------------------------
    // Public email methods — signatures unchanged
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
