package com.adc.adcampaign.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    /**
     * Name of the new tenant to create; the registering user becomes its
     * TENANT_ADMIN. Must not already exist — joining an existing tenant is
     * an invitation/administration flow, not self-registration (adding
     * yourself to another tenant by guessing its name must not be possible).
     */
    @NotBlank
    private String tenantName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "must be at least 8 characters")
    private String password;
}
