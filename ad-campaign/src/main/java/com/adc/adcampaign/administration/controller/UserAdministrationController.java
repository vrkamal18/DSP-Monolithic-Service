package com.adc.adcampaign.administration.controller;

import com.adc.adcampaign.administration.UserAdministrationService;
import com.adc.adcampaign.administration.dto.CreateUserRequest;
import com.adc.adcampaign.administration.dto.GrantRoleRequest;
import com.adc.adcampaign.administration.dto.UserResponse;
import com.adc.adcampaign.foundation.security.TenantContext;
import com.adc.adcampaign.identity.entity.Role;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class UserAdministrationController {

    private final UserAdministrationService userAdministrationService;

    public UserAdministrationController(UserAdministrationService userAdministrationService) {
        this.userAdministrationService = userAdministrationService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        var user = userAdministrationService.createUser(TenantContext.requireTenantId(),
                TenantContext.requireUserId(), request.getEmail(), request.getPassword(), request.getRole());
        return ResponseEntity.ok(UserResponse.from(user, List.of(request.getRole())));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userAdministrationService.listUsers(TenantContext.requireTenantId()).stream()
                .map(UserResponse::from)
                .toList());
    }

    @PostMapping("/{userId}/roles")
    public ResponseEntity<Void> grantRole(@PathVariable Long userId, @Valid @RequestBody GrantRoleRequest request) {
        userAdministrationService.grantRole(TenantContext.requireTenantId(), TenantContext.requireUserId(),
                userId, request.getRole());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/roles/{role}")
    public ResponseEntity<Void> revokeRole(@PathVariable Long userId, @PathVariable Role role) {
        userAdministrationService.revokeRole(TenantContext.requireTenantId(), TenantContext.requireUserId(),
                userId, role);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long userId) {
        userAdministrationService.deactivateUser(TenantContext.requireTenantId(), TenantContext.requireUserId(),
                userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/reactivate")
    public ResponseEntity<Void> reactivateUser(@PathVariable Long userId) {
        userAdministrationService.reactivateUser(TenantContext.requireTenantId(), TenantContext.requireUserId(),
                userId);
        return ResponseEntity.ok().build();
    }
}
