package com.college.bookmyslot.controller;

import com.college.bookmyslot.service.EmailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MailTestController {

    private final EmailService emailService;

    public MailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/api/mail/test")
    public String testMail() {
        try {
            String to = "deepak.unoficial18@gmail.com"; // or any email you can check
            emailService.sendOtpEmail(to, "Deepak", "123456", "Mail Test");
            return "Mail sent (check inbox/spam)";
        } catch (Exception e) {
            e.printStackTrace();
            return "Mail FAILED: " + e.getMessage();
        }
    }
}
