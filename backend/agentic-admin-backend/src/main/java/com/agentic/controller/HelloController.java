package com.agentic.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
public class HelloController {

    @GetMapping("/api/public")
    public String publicHello() {
        return "Hello from backend (public)";
    }

    @GetMapping("/api/admin")
    @PreAuthorize("hasRole('SYSADMIN')")
    public String adminHello() {
        return "Hello admin - secured";
    }
}
