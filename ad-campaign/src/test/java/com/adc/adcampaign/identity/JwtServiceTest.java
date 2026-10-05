package com.adc.adcampaign.identity;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-at-least-32-bytes-long!!";

    private final JwtService jwtService = new JwtService(SECRET, 60);

    @Test
    void issuedTokenParsesBackToTheSameClaims() {
        JwtService.IssuedToken issued = jwtService.issue(1L, 2L, Set.of("TENANT_ADMIN", "CAMPAIGN_MANAGER"));

        Optional<JwtService.ParsedToken> parsed = jwtService.parse(issued.token());

        assertThat(parsed).isPresent();
        assertThat(parsed.get().userId()).isEqualTo(1L);
        assertThat(parsed.get().tenantId()).isEqualTo(2L);
        assertThat(parsed.get().roles()).containsExactlyInAnyOrder("TENANT_ADMIN", "CAMPAIGN_MANAGER");
    }

    @Test
    void rejectsATamperedToken() {
        // Flips a character in the middle of the token rather than the very
        // last character: the last base64url character of the signature only
        // encodes a couple of padding bits, so occasionally two different
        // characters there decode to the same bytes and the "tampered" token
        // is still valid — a real, if rare, flake this avoids entirely.
        JwtService.IssuedToken issued = jwtService.issue(1L, 2L, Set.of("TENANT_ADMIN"));
        String token = issued.token();
        int middle = token.length() / 2;
        char replacement = token.charAt(middle) == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, middle) + replacement + token.substring(middle + 1);

        assertThat(jwtService.parse(tampered)).isEmpty();
    }

    @Test
    void rejectsGarbageInput() {
        assertThat(jwtService.parse("not-a-jwt")).isEmpty();
    }

    @Test
    void constructorRejectsASecretShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtService("too-short", 60))
                .isInstanceOf(IllegalStateException.class);
    }
}
