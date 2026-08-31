package com.yeniden.identity.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 80) @Pattern(regexp = ".*\\S.*", message = "Ad boş olamaz") String displayName,
        @Size(max = 255) @Pattern(regexp = "[a-zA-Z0-9/_-]+(?:\\.[a-zA-Z0-9]+)?") String avatarKey) {}

