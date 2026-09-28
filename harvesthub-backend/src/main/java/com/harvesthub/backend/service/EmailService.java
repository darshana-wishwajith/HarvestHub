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

    @Async // This annotation tells Spring to run this method on a background thread
    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("HarvestHub - Password Reset Request");
        message.setText("To reset your password, please use the following token: " + token +
                "\n\nIf you did not request a password reset, please ignore this email.");
        
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
