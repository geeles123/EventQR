package com.thedavelopers.eventqr.features.idprinting.model.dto;

import jakarta.validation.constraints.NotBlank;

public record IdTemplateRequest(@NotBlank String name, String templateJson, boolean active) {
}