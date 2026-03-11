package com.college.bookmyslot.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MailTestController {

    @Autowired
    private JavaMailSender mailSender;

    @GetMapping("/mail-test")
    public String sendTestMail() {

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("deepak.unofficial18@gmail.com");
            message.setSubject("SMTP Test");
            message.setText("Mail working from Render server");

            mailSender.send(message);

            return "Mail Sent Successfully";

        } catch (Exception e) {
            return "Mail Failed: " + e.getMessage();
        }
    }
}
