package com.yeniden.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ProblemWriter {
    private final ObjectMapper mapper;
    public ProblemWriter(ObjectMapper mapper) { this.mapper = mapper; }
    public void write(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        mapper.writeValue(response.getOutputStream(), Map.of(
                "type", "about:blank", "title", code, "status", status, "code", code));
    }
}

