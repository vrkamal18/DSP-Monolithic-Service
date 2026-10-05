package com.adc.adcampaign.administration.dto;

import com.adc.adcampaign.identity.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "must be at least 8 characters")
    private String password;

    @NotNull
    private Role role;
}
