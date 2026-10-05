package com.adc.adcampaign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// UserDetailsServiceAutoConfiguration is excluded because auth is JWT-based
// (identity.JwtAuthFilter sets the SecurityContext directly) - there is no
// UserDetailsService/AuthenticationManager in this app, so the default
// generated in-memory user is unused noise, not a real credential.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AdCampaignAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdCampaignAppApplication.class, args);
	}

}
