package com.yeniden.identity.controller;

import com.yeniden.common.result.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
public class HealthController {
    @GetMapping("/api/v1/identity/health")
    public ApiResponse<String> health() { return ApiResponse.success("Identity Service çalışıyor"); }
}

