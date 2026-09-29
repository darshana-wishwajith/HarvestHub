package com.harvesthub.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("HarvestHub - Password Reset Verification Code");
        message.setText("Hello,\n\n" +
                "Your 6-digit password reset verification code is:\n\n" +
                "    " + token + "\n\n" +
                "This code is valid for 15 minutes. Enter this code in your HarvestHub app to set a new password.\n\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "Regards,\n" +
                "HarvestHub Security Team");

        mailSender.send(message);
    }

    @Async
    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("HarvestHub - Verification Code");
        message.setText("Your HarvestHub verification code is: " + otp + "\n\nThis code will expire in 10 minutes.\n\nIf you did not request this code, please ignore this email.");
        mailSender.send(message);
    }
}