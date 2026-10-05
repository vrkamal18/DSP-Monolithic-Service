package com.adc.adcampaign.channels;

import com.adc.adcampaign.campaign.entity.Creative;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelMarkupServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChannelMarkupService markupService = new ChannelMarkupService(objectMapper);

    private Creative creativeOfType(String type) {
        return Creative.builder().id(7L).name("Test Creative").type(type)
                .url("https://cdn.example/asset.mp4").width(300).height(250).build();
    }

    @Test
    void videoCreativeProducesParseableVastContainingTheUrl() throws Exception {
        String markup = markupService.buildMarkup(creativeOfType("video"));

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(markup.getBytes(StandardCharsets.UTF_8)));

        assertThat(document.getDocumentElement().getTagName()).isEqualTo("VAST");
        assertThat(markup).contains("https://cdn.example/asset.mp4");
    }

    @Test
    void nativeCreativeProducesValidJsonContainingTheUrl() throws Exception {
        String markup = markupService.buildMarkup(creativeOfType("native"));

        JsonNode root = objectMapper.readTree(markup);

        assertThat(root.path("native").path("link").asText()).isEqualTo("https://cdn.example/asset.mp4");
        assertThat(root.path("native").path("assets").get(0).path("img").path("url").asText())
                .isEqualTo("https://cdn.example/asset.mp4");
    }

    @Test
    void displayCreativeProducesAnImgSnippetWithUrlAndDimensions() {
        String markup = markupService.buildMarkup(creativeOfType("image"));

        assertThat(markup).contains("<img");
        assertThat(markup).contains("https://cdn.example/asset.mp4");
        assertThat(markup).contains("width=\"300\"");
        assertThat(markup).contains("height=\"250\"");
    }

    @Test
    void html5CreativeProducesAnIframeSnippet() {
        String markup = markupService.buildMarkup(creativeOfType("html5"));

        assertThat(markup).contains("<iframe");
        assertThat(markup).contains("https://cdn.example/asset.mp4");
    }
}
