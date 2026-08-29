package com.yeniden.identity.controller;

import com.yeniden.common.result.ApiResponse;
import com.yeniden.identity.dto.RegisterRequest;
import com.yeniden.identity.dto.UserDto;
import com.yeniden.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Identity Service için dışa açık REST Controller katmanı.
 */
@RestController
@RequestMapping("/api/v1/identity")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Identity Service çalışıyor!"));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<UserDto>> registerOrLogin(@RequestBody RegisterRequest request) {
        UserDto dto = userService.registerOrLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Kullanıcı işlemi başarılı", dto));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable("id") UUID id) {
        UserDto dto = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/users/phone/{phone}")
    public ResponseEntity<ApiResponse<UserDto>> getUserByPhone(@PathVariable("phone") String phone) {
        UserDto dto = userService.getUserByPhone(phone);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
