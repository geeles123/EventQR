package com.thedavelopers.eventqr.features.users.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCreateRequest(@NotBlank @Email String email, @NotBlank String fullName, String phoneNumber,
                                 @NotBlank @Size(min = 8) String password) {
}