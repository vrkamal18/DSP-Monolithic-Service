package com.adc.adcampaign.channels;

import com.adc.adcampaign.campaign.entity.Creative;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

/**
 * Real VAST/native/display markup construction from a Creative — per
 * service/documents/02-module-specifications.md §10. Not wired into the live
 * OpenRTB bid response (see the phase plan for why: bidding's dependency
 * list doesn't include channels, and wiring it in would add an unsanctioned
 * dependency). Exposed instead as a standalone, testable capability useful
 * for creative QA.
 */
@Service
public class ChannelMarkupService {

    private static final String DEFAULT_VIDEO_DURATION = "00:00:15";

    private final ObjectMapper objectMapper;

    public ChannelMarkupService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildMarkup(Creative creative) {
        String type = creative.getType() == null ? "" : creative.getType().toLowerCase();
        return switch (type) {
            case "video" -> buildVast(creative);
            case "native" -> buildNative(creative);
            case "html5" -> buildIframe(creative);
            default -> buildImage(creative);
        };
    }

    private String buildVast(Creative creative) {
        int width = creative.getWidth() == null ? 0 : creative.getWidth();
        int height = creative.getHeight() == null ? 0 : creative.getHeight();
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <VAST version="3.0">
                  <Ad id="%d">
                    <InLine>
                      <AdSystem>AdCampaign</AdSystem>
                      <AdTitle>%s</AdTitle>
                      <Impression><![CDATA[]]></Impression>
                      <Creatives>
                        <Creative>
                          <Linear>
                            <Duration>%s</Duration>
                            <MediaFiles>
                              <MediaFile delivery="progressive" type="video/mp4" width="%d" height="%d"><![CDATA[%s]]></MediaFile>
                            </MediaFiles>
                          </Linear>
                        </Creative>
                      </Creatives>
                    </InLine>
                  </Ad>
                </VAST>
                """.formatted(creative.getId(), escapeXml(creative.getName()), DEFAULT_VIDEO_DURATION,
                width, height, creative.getUrl());
    }

    private String buildNative(Creative creative) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode nativeNode = root.putObject("native");
        var assets = nativeNode.putArray("assets");
        ObjectNode imageAsset = assets.addObject();
        imageAsset.put("id", 1);
        ObjectNode img = imageAsset.putObject("img");
        img.put("url", creative.getUrl());
        img.put("w", creative.getWidth() == null ? 0 : creative.getWidth());
        img.put("h", creative.getHeight() == null ? 0 : creative.getHeight());
        nativeNode.put("link", creative.getUrl());
        return root.toString();
    }

    private String buildIframe(Creative creative) {
        return "<iframe src=\"%s\" width=\"%d\" height=\"%d\" frameborder=\"0\" scrolling=\"no\"></iframe>"
                .formatted(creative.getUrl(),
                        creative.getWidth() == null ? 0 : creative.getWidth(),
                        creative.getHeight() == null ? 0 : creative.getHeight());
    }

    private String buildImage(Creative creative) {
        return "<img src=\"%s\" width=\"%d\" height=\"%d\" alt=\"\"/>"
                .formatted(creative.getUrl(),
                        creative.getWidth() == null ? 0 : creative.getWidth(),
                        creative.getHeight() == null ? 0 : creative.getHeight());
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
