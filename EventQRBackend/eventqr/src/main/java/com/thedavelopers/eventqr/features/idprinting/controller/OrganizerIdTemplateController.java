package com.thedavelopers.eventqr.features.idprinting.controller;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thedavelopers.eventqr.features.idprinting.model.dto.IdPrintResponse;
import com.thedavelopers.eventqr.features.idprinting.model.dto.IdTemplateRequest;
import com.thedavelopers.eventqr.features.idprinting.model.entity.IdTemplate;
import com.thedavelopers.eventqr.features.idprinting.service.IdPrintingService;
import com.thedavelopers.eventqr.shared.constants.AccountRole;
import com.thedavelopers.eventqr.shared.response.ApiResponse;
import com.thedavelopers.eventqr.shared.security.JwtService;

@RestController
@RequestMapping("/api/v1")
public class OrganizerIdTemplateController {

    private final IdPrintingService idPrintingService;
    private final JwtService jwtService;

    public OrganizerIdTemplateController(IdPrintingService idPrintingService, JwtService jwtService) {
        this.idPrintingService = idPrintingService;
        this.jwtService = jwtService;
    }

    @GetMapping("/organizer/events/{eventId}/id-templates")
    public ResponseEntity<ApiResponse<List<IdTemplate>>> list(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(idPrintingService.listTemplates(eventId)));
    }

    @PostMapping("/organizer/events/{eventId}/id-template")
    public ResponseEntity<ApiResponse<IdTemplate>> create(@PathVariable UUID eventId,
                                                          @Valid @RequestBody IdTemplateRequest body) {
        return ResponseEntity.ok(ApiResponse.success("ID template saved", idPrintingService.saveTemplate(eventId, body)));
    }

    @GetMapping("/organizer/events/{eventId}/id-template")
    public ResponseEntity<ApiResponse<IdTemplate>> get(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(idPrintingService.getTemplate(eventId)));
    }

    @PatchMapping("/organizer/events/{eventId}/id-template")
    public ResponseEntity<ApiResponse<IdTemplate>> update(@PathVariable UUID eventId,
                                                          @Valid @RequestBody IdTemplateRequest body) {
        return ResponseEntity.ok(ApiResponse.success("ID template updated", idPrintingService.saveTemplate(eventId, body)));
    }

    @GetMapping("/organizer/events/{eventId}/id-template/preview")
    public ResponseEntity<ApiResponse<IdTemplate>> preview(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success(idPrintingService.getTemplate(eventId)));
    }

    @PostMapping("/organizer/events/{eventId}/id-template/logo")
    public ResponseEntity<ApiResponse<Void>> uploadLogo() {
        return ResponseEntity.status(501).body(new ApiResponse<>(false, "Logo upload is not wired yet", null, java.time.Instant.now()));
    }

    @PostMapping("/staff/events/{eventId}/attendees/{attendeeId}/id-preview")
    public ResponseEntity<ApiResponse<IdPrintResponse>> previewForAttendee(HttpServletRequest request,
                                                                           @PathVariable UUID eventId,
                                                                           @PathVariable UUID attendeeId) {
        requireNonAttendee(request);
        return ResponseEntity.ok(ApiResponse.success(idPrintingService.previewForAttendee(eventId, attendeeId)));
    }

    @PostMapping("/staff/events/{eventId}/attendees/{attendeeId}/print-id")
    public ResponseEntity<ApiResponse<IdPrintResponse>> print(HttpServletRequest request,
                                                              @PathVariable UUID eventId,
                                                              @PathVariable UUID attendeeId) {
        requireNonAttendee(request);
        return ResponseEntity.ok(ApiResponse.success(idPrintingService.printForAttendee(eventId, attendeeId, false)));
    }

    @PostMapping("/staff/events/{eventId}/attendees/{attendeeId}/reprint-id")
    public ResponseEntity<ApiResponse<IdPrintResponse>> reprint(HttpServletRequest request,
                                                                @PathVariable UUID eventId,
                                                                @PathVariable UUID attendeeId) {
        requireNonAttendee(request);
        return ResponseEntity.ok(ApiResponse.success(idPrintingService.printForAttendee(eventId, attendeeId, true)));
    }

    private void requireNonAttendee(HttpServletRequest request) {
        if (jwtService.extractRoleFromBearer(request.getHeader("Authorization")) == AccountRole.ATTENDEE) {
            throw new com.thedavelopers.eventqr.shared.exception.ForbiddenException("Staff or organizer access required");
        }
    }
}