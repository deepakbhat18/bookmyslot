package com.college.bookmyslot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

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
        text.append("\nPlease verify your email using the OTP sent in a separate mail.\n\n");
        text.append("Regards,\nBookMySlot Team");

        message.setText(text.toString());

        mailSender.send(message);
    }

    public void sendOtpEmail(String to, String name, String otp, String purpose) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Your OTP for " + purpose);

        StringBuilder text = new StringBuilder();
        text.append("Hi ").append(name).append(",\n\n");
        text.append("Your OTP for ").append(purpose).append(" is: ").append(otp).append("\n");
        text.append("This OTP is valid for 10 minutes.\n\n");
        text.append("If you did not request this, please ignore.\n\n");
        text.append("Regards,\nBookMySlot Team");

        message.setText(text.toString());

        mailSender.send(message);
    }

    public void sendSlotBookingEmail(String to, String name, String teacherName,
                                     String date, String startTime, String endTime) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Slot Booking Confirmation");

        StringBuilder text = new StringBuilder();
        text.append("Hi ").append(name).append(",\n\n");
        text.append("Your slot has been booked with ").append(teacherName).append(".\n");
        text.append("Date: ").append(date).append("\n");
        text.append("Time: ").append(startTime).append(" - ").append(endTime).append("\n\n");
        text.append("Regards,\nBookMySlot Team");

        message.setText(text.toString());

        mailSender.send(message);
    }
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

        StringBuilder text = new StringBuilder();
        text.append("Hi ").append(teacherName).append(",\n\n");
        text.append("A student has booked one of your slots.\n\n");
        text.append("Student details:\n");
        text.append("Name: ").append(studentName).append("\n");
        text.append("Email: ").append(studentEmail).append("\n");
        if (studentUsn != null && !studentUsn.isBlank()) {
            text.append("USN: ").append(studentUsn).append("\n");
        }
        text.append("\nSlot details:\n");
        text.append("Date: ").append(date).append("\n");
        text.append("Time: ").append(startTime).append(" - ").append(endTime).append("\n\n");
        text.append("Regards,\nBookMySlot System");

        message.setText(text.toString());

        mailSender.send(message);
    }

}
