package com.thedavelopers.eventqr.features.users.model.dto;

import com.thedavelopers.eventqr.shared.constants.AccountStatus;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(@NotNull AccountStatus status) {
}