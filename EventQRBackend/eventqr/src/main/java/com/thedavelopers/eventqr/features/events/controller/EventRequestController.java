package com.thedavelopers.eventqr.features.events.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thedavelopers.eventqr.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/event-requests")
public class EventRequestController {

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create() {
        return notImplemented("Event request persistence is not wired yet");
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Void>> mine() {
        return notImplemented("Event request persistence is not wired yet");
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<Void>> findOne(@PathVariable UUID requestId) {
        return notImplemented("Event request persistence is not wired yet");
    }

    private ResponseEntity<ApiResponse<Void>> notImplemented(String message) {
        return ResponseEntity.status(501).body(new ApiResponse<>(false, message, null, java.time.Instant.now()));
    }
}