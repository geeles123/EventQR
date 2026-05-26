package com.thedavelopers.eventqr.features.users.model.dto;

import com.thedavelopers.eventqr.shared.constants.AccountRole;

import jakarta.validation.constraints.NotNull;

public record UserRoleRequest(@NotNull AccountRole role) {
}