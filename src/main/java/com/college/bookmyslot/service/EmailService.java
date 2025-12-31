package com.college.bookmyslot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String fromEmail;
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void sendWelcomeEmail(String to, String name, String role, String usn) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Welcome to BookMySlot");
        StringBuilder text = new StringBuilder();
        text.append("Hi ").append(name).append(",\n\n");
        text.append("Your BookMySlot account has been created successfully.\n");
        text.append("Role: ").append(role).append("\n");
        if (usn != null && !usn.isBlank()) {
            text.append("USN: ").append(usn).append("\n");
        }
        text.append("\nRegards,\nBookMySlot Team");

        message.setText(text.toString());
        mailSender.send(message);
    }

    @Async
    @Retryable(
            value = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    public void sendOtpEmail(String to, String name, String otp, String purpose) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("BookMySlot OTP - " + purpose);

        message.setText(
                "Hi " + name + ",\n\n" +
                        "Your OTP for " + purpose + " is: " + otp + "\n\n" +
                        "This OTP is valid for 10 minutes.\n\n" +
                        "Regards,\nBookMySlot Team"
        );

        mailSender.send(message);
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Slot Booking Confirmation");

        message.setText(
                "Hi " + name + ",\n\n" +
                        "Your slot has been booked with " + teacherName + ".\n" +
                        "Date: " + date + "\n" +
                        "Time: " + startTime + " - " + endTime + "\n\n" +
                        "Regards,\nBookMySlot Team"
        );

        mailSender.send(message);
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("New slot booking from " + studentName);

        message.setText(
                "Hi " + teacherName + ",\n\n" +
                        "A student has booked one of your slots.\n\n" +
                        "Student: " + studentName + "\n" +
                        "Email: " + studentEmail + "\n" +
                        (studentUsn != null && !studentUsn.isBlank()
                                ? "USN: " + studentUsn + "\n"
                                : "") +
                        "\nDate: " + date + "\n" +
                        "Time: " + startTime + " - " + endTime + "\n\n" +
                        "Regards,\nBookMySlot Team"
        );

        mailSender.send(message);
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Event Booking Confirmed - " + eventTitle);

        message.setText(
                "Hi " + studentName + ",\n\n" +
                        "Your booking is confirmed 🎉\n\n" +
                        "Event: " + eventTitle + "\n" +
                        "Venue: " + venue + "\n" +
                        "Date: " + eventDate + "\n" +
                        "Time: " + eventTime + "\n" +
                        "Ticket ID: " + ticketId + "\n\n" +
                        "Regards,\nBookMySlot Team"
        );

        mailSender.send(message);
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("⏰ Reminder: Event starts soon");

        message.setText(
                "Hi " + studentName + ",\n\n" +
                        "Reminder: \"" + eventTitle + "\" starts at " + startTime + ".\n\n" +
                        "Venue: " + venue + "\n\n" +
                        "— BookMySlot Team"
        );

        mailSender.send(message);
    }

    @Recover
    public void recover(Exception ex, Object... args) {
        System.err.println("❌ Email failed after retries: " + ex.getMessage());

    }
}
