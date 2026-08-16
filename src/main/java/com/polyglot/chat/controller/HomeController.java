package com.polyglot.chat.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Polyglot Chat API");
        response.put("status", "running");

        Map<String, String> auth = new HashMap<>();
        auth.put("otpLogin", "POST /api/auth/send-otp");
        auth.put("googleLogin", "GET /oauth2/authorization/google");
        response.put("authentication", auth);

        return response;
    }
}