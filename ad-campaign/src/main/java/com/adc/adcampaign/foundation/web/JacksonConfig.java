package com.adc.adcampaign.foundation.web;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Hibernate6Module so uninitialized LAZY associations on JPA
 * entities (which the campaign/advertiser/etc. controllers still return
 * directly, pending a proper DTO layer) serialize as null instead of
 * failing on the Hibernate proxy class itself.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate6Module hibernate6Module() {
        return new Hibernate6Module();
    }
}
