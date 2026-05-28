package com.thedavelopers.eventqr.features.reports.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thedavelopers.eventqr.features.organizer.model.dto.OrganizerDtos.OrganizerReportResponse;
import com.thedavelopers.eventqr.features.organizer.service.OrganizerService;
import com.thedavelopers.eventqr.features.reports.service.ReportService;
import com.thedavelopers.eventqr.features.reports.model.dto.EventReportSnapshot;
import com.thedavelopers.eventqr.shared.response.ApiResponse;
import com.thedavelopers.eventqr.shared.security.JwtService;

@RestController
@RequestMapping("/api/v1/organizer/events/{eventId}/reports")
public class OrganizerReportController {

    private final ReportService reportService;
    private final OrganizerService organizerService;
    private final JwtService jwtService;

    public OrganizerReportController(ReportService reportService, OrganizerService organizerService, JwtService jwtService) {
        this.reportService = reportService;
        this.organizerService = organizerService;
        this.jwtService = jwtService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<OrganizerReportResponse>> summary(HttpServletRequest request, @PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(organizerService.report(currentUserId(request), eventId)));
    }


    @GetMapping("/attendance")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> attendance(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generate(eventId)));
    }

    @GetMapping("/entries")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> entries(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generate(eventId)));
    }

    @GetMapping("/exits")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> exits(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generate(eventId)));
    }

    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> claims(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generate(eventId)));
    }

    @GetMapping("/booth-visits")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> boothVisits(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generate(eventId)));
    }

    @GetMapping("/rewards")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> rewards(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generate(eventId)));
    }

    @GetMapping("/points")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> points(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generate(eventId)));
    }

    @PostMapping("/export")
    public ResponseEntity<ApiResponse<EventReportSnapshot>> export(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success("Export prepared", reportService.generate(eventId)));
    }

    private UUID currentUserId(HttpServletRequest request) {
        return jwtService.extractUserIdFromBearer(request.getHeader("Authorization"));
    }
}