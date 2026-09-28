package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.GenericResponse;
import com.harvesthub.backend.dto.ResendOtpRequest;
import com.harvesthub.backend.dto.SendOtpRequest;
import com.harvesthub.backend.dto.VerifyOtpRequest;
import com.harvesthub.backend.dto.VerifyOtpResponse;
import com.harvesthub.backend.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
public class OtpController {
    @Autowired
    private OtpService otpService;

    @PostMapping("/send-otp")
    public ResponseEntity<GenericResponse> sendOtp(@RequestBody SendOtpRequest request) {
        GenericResponse response = otpService.sendOtp(request.getIdentifier());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody ResendOtpRequest request) {
        GenericResponse response = otpService.resendOtp(request.getIdentifier());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse response = otpService.verifyOtp(request.getUserId(), request.getOtp());
        return ResponseEntity.ok(response);
    }
}
