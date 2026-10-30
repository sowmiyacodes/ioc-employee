package com.essp.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminUserStatusRequest(@NotNull Boolean active) {
}
