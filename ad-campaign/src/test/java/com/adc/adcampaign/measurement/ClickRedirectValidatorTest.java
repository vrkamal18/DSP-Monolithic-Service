package com.adc.adcampaign.measurement;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClickRedirectValidatorTest {

    private final ClickRedirectValidator validator = new ClickRedirectValidator();

    @Test
    void acceptsWellFormedAbsoluteHttpsUrl() {
        assertThat(validator.isSafe("https://example.com/landing")).isTrue();
    }

    @Test
    void acceptsWellFormedAbsoluteHttpUrl() {
        assertThat(validator.isSafe("http://example.com")).isTrue();
    }

    @Test
    void rejectsJavascriptScheme() {
        assertThat(validator.isSafe("javascript:alert(1)")).isFalse();
    }

    @Test
    void rejectsDataScheme() {
        assertThat(validator.isSafe("data:text/html,<script>alert(1)</script>")).isFalse();
    }

    @Test
    void rejectsRelativePath() {
        assertThat(validator.isSafe("/relative/path")).isFalse();
    }

    @Test
    void rejectsBlank() {
        assertThat(validator.isSafe("")).isFalse();
        assertThat(validator.isSafe(null)).isFalse();
    }

    @Test
    void rejectsMalformedUrl() {
        assertThat(validator.isSafe("ht!tp://[invalid")).isFalse();
    }
}
