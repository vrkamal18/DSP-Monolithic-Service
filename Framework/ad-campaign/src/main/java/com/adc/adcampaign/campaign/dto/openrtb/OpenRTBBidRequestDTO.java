package com.adc.adcampaign.campaign.dto.openrtb;

import lombok.Data;

@Data
public class OpenRTBBidRequestDTO {
    private String id; // Unique request ID

    private Imp imp; // Impression object
    private Device device; // Device info
    private User user; // User info

    @Data
    public static class Imp {
        private String id;
        private Banner banner;

        @Data
        public static class Banner {
            private Integer w;
            private Integer h;
        }
    }

    @Data
    public static class Device {
        private String ua;
        private String ip;
        private String geo;
        private String devicetype; // e.g., "mobile", "desktop"
    }

    @Data
    public static class User {
        private String id;
        private Integer yob;
        private String gender;
    }
}

