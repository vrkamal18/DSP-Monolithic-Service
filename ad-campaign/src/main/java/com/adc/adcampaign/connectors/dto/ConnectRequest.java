package com.adc.adcampaign.connectors.dto;

import com.adc.adcampaign.connectors.entity.ConnectorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConnectRequest {
    @NotNull
    private ConnectorType type;

    @NotBlank
    private String externalAccountId;

    @NotBlank
    private String credential;
}
