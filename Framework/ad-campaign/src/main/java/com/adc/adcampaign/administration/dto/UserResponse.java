package com.adc.adcampaign.administration.dto;

import com.adc.adcampaign.administration.UserAdministrationService;
import com.adc.adcampaign.identity.entity.AppUser;
import com.adc.adcampaign.identity.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private boolean enabled;
    private List<Role> roles;
    private LocalDateTime createdAt;

    public static UserResponse from(AppUser user, List<Role> roles) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static UserResponse from(UserAdministrationService.UserWithRoles userWithRoles) {
        return UserResponse.builder()
                .id(userWithRoles.user().getId())
                .email(userWithRoles.user().getEmail())
                .enabled(userWithRoles.user().isEnabled())
                .roles(userWithRoles.roles())
                .createdAt(userWithRoles.user().getCreatedAt())
                .build();
    }
}
