package com.adc.adcampaign.administration.dto;

import com.adc.adcampaign.identity.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GrantRoleRequest {
    @NotNull
    private Role role;
}
