package com.chronos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RescheduleRequest {
    @NotBlank(message = "Schedule is required")
    private String schedule;

    private Boolean isRecurring;
}

