package com.college.bookmyslot.controller;

import com.college.bookmyslot.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MailTestController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/mail-test")
    public String sendTestMail() {

        try {
            emailService.sendOtpEmail(
                    "deepak.unofficial18@gmail.com",
                    "Test",
                    "123456",
                    "SMTP Test"
            );

            return "Mail Sent Successfully";

        } catch (Exception e) {
            return "Mail Failed: " + e.getMessage();
        }
    }
}
